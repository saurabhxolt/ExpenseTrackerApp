package com.expensetracker.app.features.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.CategoryRepository
import com.expensetracker.app.core.data.repository.SubscriptionRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.entity.SubscriptionEntity
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.core.promotions.Promotion
import com.expensetracker.app.core.promotions.PromotionManager
import com.expensetracker.app.core.utils.PermissionUtils
import com.expensetracker.app.ingestion.parser.CategoryClassifier
import com.expensetracker.app.ingestion.sms.SmsScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class PeriodFilter(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_QUARTER("This Quarter"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

data class DashboardUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedPeriodFilter: PeriodFilter = PeriodFilter.THIS_MONTH,
    val monthYearOptions: List<String> = listOf("ALL"),
    val selectedMonthYear: String = "ALL",
    val selectedSortOrder: String = "NEWEST", // NEWEST, OLDEST, HIGH_LOW, LOW_HIGH
    val todayExpenses: Double = 0.0,
    val thisWeekExpenses: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalInvestments: Double = 0.0,
    val netCashflow: Double = 0.0,
    val savingsRate: Double = 0.0,
    val avgDailySpend: Double = 0.0,
    val lastDeletedTransaction: TransactionEntity? = null,
    val promotions: List<Promotion> = emptyList(),
    val isAutoTrackingEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val upcomingSubscriptions: List<SubscriptionEntity> = emptyList(),
    val totalMonthlyCommitted: Double = 0.0,
    val totalPaidCommitted: Double = 0.0,
    val totalPendingCommitted: Double = 0.0,
    val safeToSpendToday: Double = 0.0,
    val daysRemainingInMonth: Int = 1,
    val totalBudgetLimit: Double = 0.0,
    val remainingMonthlyBudget: Double = 0.0,
    val hasBudgetSet: Boolean = false,
    val totalPendingBillsThisMonth: Double = 0.0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val budgetRepository: com.expensetracker.app.core.data.repository.BudgetRepository,
    private val creditCardRepository: com.expensetracker.app.core.data.repository.CreditCardRepository,
    private val savingsGoalRepository: com.expensetracker.app.core.data.repository.SavingsGoalRepository,
    private val recurringIncomeRepository: com.expensetracker.app.core.data.repository.RecurringIncomeRepository,
    private val insurancePolicyRepository: com.expensetracker.app.core.data.repository.InsurancePolicyRepository,
    private val promotionManager: PromotionManager,
    private val categoryClassifier: CategoryClassifier,
    val receiptOcrParser: com.expensetracker.app.core.ocr.ReceiptOcrParser
) : ViewModel() {

    val registeredCards: StateFlow<List<com.expensetracker.app.core.database.entity.CreditCardEntity>> = creditCardRepository.allCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<com.expensetracker.app.core.database.entity.SavingsGoalEntity>> = savingsGoalRepository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringIncomes: StateFlow<List<com.expensetracker.app.core.database.entity.RecurringIncomeEntity>> = recurringIncomeRepository.allIncomes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insurancePolicies: StateFlow<List<com.expensetracker.app.core.database.entity.InsurancePolicyEntity>> = insurancePolicyRepository.allPolicies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salaryProvisioningPlan: StateFlow<com.expensetracker.app.core.analysis.SalaryProvisioningPlan> = combine(
        insurancePolicies,
        savingsGoals
    ) { policies, jars ->
        com.expensetracker.app.core.analysis.SalaryProvisioningCalculator.calculatePlan(policies, jars)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.expensetracker.app.core.analysis.SalaryProvisioningPlan())

    private val _isScanning = MutableStateFlow(false)
    private val _isAutoTrackingEnabled = MutableStateFlow(false)
    private val _selectedPeriodFilter = MutableStateFlow(PeriodFilter.THIS_MONTH)
    private val _selectedMonthYear = MutableStateFlow("ALL")
    private val _selectedSortOrder = MutableStateFlow("NEWEST")
    private val _lastDeletedTransaction = MutableStateFlow<TransactionEntity?>(null)
    private val _promotions = MutableStateFlow<List<Promotion>>(emptyList())

    private val _filterState = combine(
        combine(_selectedPeriodFilter, _selectedMonthYear, _selectedSortOrder) { p, m, s -> Triple(p, m, s) },
        combine(_isAutoTrackingEnabled, _isScanning, _lastDeletedTransaction, _promotions) { a, sc, l, pr ->
            FilterExtra(a, sc, l, pr)
        }
    ) { pms, extra ->
        FilterState(
            periodFilter = pms.first,
            monthYear = pms.second,
            sortOrder = pms.third,
            isAutoTrackingEnabled = extra.autoTrack,
            isScanning = extra.scanning,
            lastDeleted = extra.lastDeleted,
            promos = extra.promos
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allTransactions,
        categoryRepository.allCategories,
        subscriptionRepository.allSubscriptions,
        budgetRepository.allBudgets,
        _filterState
    ) { transactions, categoryEntities, subscriptions, budgets, filterState ->

        val startOfToday = getStartOfTodayTimestamp()
        val startOf7DaysAgo = getStartOf7DaysAgoTimestamp()

        val todayExp = transactions.filter { it.type == "DEBIT" && it.timestamp >= startOfToday }.sumOf { it.amount }
        val weekExp = transactions.filter { it.type == "DEBIT" && it.timestamp >= startOf7DaysAgo }.sumOf { it.amount }

        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val availableMonths = transactions
            .map { monthFormat.format(Date(it.timestamp)) }
            .distinct()
            .sortedDescending()
        val allMonthOptions = listOf("ALL") + availableMonths

        val (periodStartMs, periodEndMs) = getPeriodTimeRange(filterState.periodFilter)

        val periodFiltered = transactions.filter { trx ->
            trx.timestamp in periodStartMs..periodEndMs
        }

        val sortedTransactions = when (filterState.sortOrder) {
            "OLDEST" -> periodFiltered.sortedBy { it.timestamp }
            "HIGH_LOW" -> periodFiltered.sortedByDescending { it.amount }
            "LOW_HIGH" -> periodFiltered.sortedBy { it.amount }
            else -> periodFiltered.sortedByDescending { it.timestamp }
        }

        val totalExp = periodFiltered.filter { it.type == "DEBIT" }.sumOf { it.amount }
        val totalInc = periodFiltered.filter { it.type == "CREDIT" }.sumOf { it.amount }
        val totalInv = periodFiltered.filter { it.type == "INVESTMENT" }.sumOf { it.amount }

        val netCash = totalInc - totalExp
        val savRate = if (totalInc > 0.0) ((totalInc - totalExp) / totalInc * 100.0).coerceIn(-100.0, 100.0) else 0.0

        val daysCount = calculateDaysInPeriod(filterState.periodFilter, periodStartMs, periodEndMs)
        val dailyAvg = if (daysCount > 0) totalExp / daysCount else totalExp

        val defaultCats = listOf("General Expense", "Food & Dining", "Transportation", "Shopping", "Bills & Utilities", "Rent", "EMI", "Groceries & Vegetables", "Subscriptions", "Entertainment", "Fuel", "Health & Medical", "Insurance", "Government & Fees", "Investments", "Income", "Transfer", "Other")
        val customCatNames = categoryEntities.map { it.name }
        val combinedCategories = (defaultCats + customCatNames).distinct()

        val activeSubs = subscriptions.filter { it.isActive }
        val totalMonthlyComm = activeSubs.sumOf { sub ->
            when (sub.billingCycle) {
                "Yearly" -> sub.amount / 12.0
                "Weekly" -> sub.amount * 4.33
                "Quarterly" -> sub.amount / 3.0
                else -> sub.amount
            }
        }
        val nowMs = System.currentTimeMillis()
        val totalPaidComm = activeSubs.filter { it.autoMatched && it.lastPaidDateMs != null && (nowMs - it.lastPaidDateMs) < 30 * 86400000L }.sumOf { it.amount }
        val totalPendingComm = (totalMonthlyComm - totalPaidComm).coerceAtLeast(0.0)

        val upcomingSubs = activeSubs.sortedBy { it.nextDueDateMs }.take(4)

        // Safe-to-Spend Today Radar Calculations (Current Calendar Month Scoped)
        val calendar = Calendar.getInstance()
        val totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysRemaining = (totalDaysInMonth - currentDay + 1).coerceAtLeast(1)

        val monthStartCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentMonthStartMs = monthStartCal.timeInMillis

        val currentMonthDebits = transactions.filter { it.type == "DEBIT" && it.timestamp >= currentMonthStartMs }.sumOf { it.amount }
        val currentMonthIncome = transactions.filter { it.type == "CREDIT" && it.timestamp >= currentMonthStartMs }.sumOf { it.amount }

        val totalBudget = budgets.sumOf { it.limitAmount }
        val hasBudget = totalBudget > 0.0

        val (safeSpendToday, remainingBudget) = if (hasBudget) {
            val rem = (totalBudget - currentMonthDebits - totalPendingComm).coerceAtLeast(0.0)
            Pair(rem / daysRemaining, rem)
        } else {
            if (currentMonthIncome > 0.0) {
                val rem = (currentMonthIncome - currentMonthDebits - totalPendingComm).coerceAtLeast(0.0)
                Pair(rem / daysRemaining, rem)
            } else {
                Pair(0.0, 0.0)
            }
        }

        DashboardUiState(
            transactions = sortedTransactions,
            categories = combinedCategories,
            selectedPeriodFilter = filterState.periodFilter,
            monthYearOptions = allMonthOptions,
            selectedMonthYear = filterState.monthYear,
            selectedSortOrder = filterState.sortOrder,
            todayExpenses = todayExp,
            thisWeekExpenses = weekExp,
            totalExpenses = totalExp,
            totalIncome = totalInc,
            totalInvestments = totalInv,
            netCashflow = netCash,
            savingsRate = savRate,
            avgDailySpend = dailyAvg,
            lastDeletedTransaction = filterState.lastDeleted,
            promotions = filterState.promos,
            isAutoTrackingEnabled = filterState.isAutoTrackingEnabled,
            isScanning = filterState.isScanning,
            upcomingSubscriptions = upcomingSubs,
            totalMonthlyCommitted = totalMonthlyComm,
            totalPaidCommitted = totalPaidComm,
            totalPendingCommitted = totalPendingComm,
            safeToSpendToday = safeSpendToday,
            daysRemainingInMonth = daysRemaining,
            totalBudgetLimit = totalBudget,
            remainingMonthlyBudget = remainingBudget,
            hasBudgetSet = hasBudget,
            totalPendingBillsThisMonth = totalPendingComm
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    init {
        loadPromotions()
    }

    fun refreshPermissionState(context: Context) {
        val hasSms = PermissionUtils.isSmsPermissionGranted(context)
        val hasNotif = PermissionUtils.isNotificationListenerGranted(context)
        _isAutoTrackingEnabled.value = hasSms && hasNotif
    }

    fun triggerSmsInboxScan(context: Context) {
        if (!PermissionUtils.isSmsPermissionGranted(context)) return
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                SmsScanner.scanInbox(context)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    private fun loadPromotions() {
        viewModelScope.launch {
            _promotions.value = promotionManager.getActivePromotions()
        }
    }

    fun setPeriodFilter(filter: PeriodFilter) {
        _selectedPeriodFilter.value = filter
    }

    fun setMonthYearFilter(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    fun setSortOrder(sortOrder: String) {
        _selectedSortOrder.value = sortOrder
    }

    fun addTransaction(
        merchant: String,
        amount: Double,
        type: String = "DEBIT",
        category: String = "General Expense",
        note: String = "",
        timestamp: Long = System.currentTimeMillis(),
        accountDigits: String = ""
    ) {
        addManualTransaction(merchant, category, amount, type, note, timestamp, accountDigits)
    }

    fun addManualTransaction(
        merchant: String,
        category: String,
        amount: Double,
        type: String = "DEBIT",
        note: String = "",
        timestamp: Long = System.currentTimeMillis(),
        accountDigits: String = ""
    ) {
        viewModelScope.launch {
            val hash = "manual_${timestamp}_${amount}_${merchant}_${accountDigits}"
            val entity = TransactionEntity(
                amount = amount,
                type = type,
                merchant = merchant,
                rawText = if (accountDigits.isNotEmpty()) "Manual Entry: $merchant ₹$amount (Card $accountDigits)" else "Manual Entry: $merchant ₹$amount",
                category = category,
                timestamp = timestamp,
                note = note,
                accountDigits = accountDigits,
                transactionHash = hash,
                isManual = true
            )
            repository.insertTransaction(entity)
            categoryClassifier.saveUserMerchantPreference(merchant, category)
        }
    }

    fun updateTransaction(transaction: TransactionEntity, newMerchant: String, newCategory: String, newAmount: Double, newType: String = transaction.type) {
        viewModelScope.launch {
            val updated = transaction.copy(
                merchant = newMerchant,
                category = newCategory,
                amount = newAmount,
                type = newType
            )
            repository.updateTransaction(updated)
            categoryClassifier.saveUserMerchantPreference(newMerchant, newCategory)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            _lastDeletedTransaction.value = transaction
            repository.deleteTransaction(transaction)
        }
    }

    fun restoreLastDeletedTransaction() {
        viewModelScope.launch {
            _lastDeletedTransaction.value?.let { trx ->
                repository.insertTransaction(trx)
                _lastDeletedTransaction.value = null
            }
        }
    }

    fun saveCashSpend(amount: Double, category: String, merchant: String, note: String) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                amount = amount,
                type = "DEBIT",
                merchant = if (merchant.isBlank()) "Cash Spend" else merchant,
                rawText = "Cash spend: $note",
                category = category,
                timestamp = System.currentTimeMillis(),
                note = note,
                isManual = true,
                transactionHash = "cash_${System.currentTimeMillis()}_${(1000..9999).random()}"
            )
            repository.insertTransaction(tx)
        }
    }

    fun markIncomeReceived(income: com.expensetracker.app.core.database.entity.RecurringIncomeEntity, recordInLedger: Boolean = true) {
        viewModelScope.launch {
            var txId: Long? = null
            if (recordInLedger) {
                val tx = TransactionEntity(
                    amount = income.amount,
                    type = "CREDIT",
                    merchant = income.name,
                    category = income.category,
                    rawText = "Manual recurring income received: ${income.name}",
                    timestamp = System.currentTimeMillis(),
                    transactionHash = "MANUAL_INC_${income.id}_${System.currentTimeMillis()}",
                    isManual = true
                )
                txId = repository.insertTransaction(tx)
            }
            recurringIncomeRepository.markReceived(income, txId)
        }
    }

    fun stashSalaryToJar(jarId: Long, amount: Double, recordInLedger: Boolean = true) {
        viewModelScope.launch {
            savingsGoalRepository.depositAmount(jarId, amount)
            if (recordInLedger) {
                val jar = savingsGoalRepository.getGoalById(jarId)
                val tx = TransactionEntity(
                    amount = amount,
                    type = "INVESTMENT",
                    merchant = jar?.title ?: "Savings Jar",
                    category = "Investments",
                    rawText = "Salary Stash deposit to ${jar?.title ?: "Savings Jar"}",
                    timestamp = System.currentTimeMillis(),
                    transactionHash = "JAR_DEP_${jarId}_${System.currentTimeMillis()}",
                    isManual = true
                )
                repository.insertTransaction(tx)
            }
        }
    }

    private fun getStartOfTodayTimestamp(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getStartOf7DaysAgoTimestamp(): Long {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -7)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun getPeriodTimeRange(period: PeriodFilter): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        return when (period) {
            PeriodFilter.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            PeriodFilter.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            PeriodFilter.THIS_QUARTER -> {
                val currentMonth = cal.get(Calendar.MONTH) // 0-11
                val firstMonthOfQuarter = (currentMonth / 3) * 3
                cal.set(Calendar.MONTH, firstMonthOfQuarter)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            PeriodFilter.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            PeriodFilter.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
        }
    }

    private fun calculateDaysInPeriod(period: PeriodFilter, startMs: Long, endMs: Long): Int {
        if (period == PeriodFilter.ALL_TIME) {
            return 30
        }
        val diffMs = (endMs - startMs).coerceAtLeast(86400000L)
        return (diffMs / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
    }

    private data class FilterExtra(
        val autoTrack: Boolean,
        val scanning: Boolean,
        val lastDeleted: TransactionEntity?,
        val promos: List<Promotion>
    )

    private data class FilterState(
        val periodFilter: PeriodFilter,
        val monthYear: String,
        val sortOrder: String,
        val isAutoTrackingEnabled: Boolean,
        val isScanning: Boolean,
        val lastDeleted: TransactionEntity?,
        val promos: List<Promotion>
    )
}
