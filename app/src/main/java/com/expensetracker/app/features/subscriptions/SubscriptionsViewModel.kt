package com.expensetracker.app.features.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.analysis.PriceCreepDetector
import com.expensetracker.app.core.analysis.PriceHikeAlert
import com.expensetracker.app.core.analysis.SalaryProvisioningCalculator
import com.expensetracker.app.core.analysis.SalaryProvisioningPlan
import com.expensetracker.app.core.analysis.PolicyProvisioningItem
import com.expensetracker.app.core.data.repository.CategoryRepository
import com.expensetracker.app.core.data.repository.DetectedSubscriptionCandidate
import com.expensetracker.app.core.data.repository.InsurancePolicyRepository
import com.expensetracker.app.core.data.repository.RecurringIncomeRepository
import com.expensetracker.app.core.data.repository.SavingsGoalRepository
import com.expensetracker.app.core.data.repository.SubscriptionDetector
import com.expensetracker.app.core.data.repository.SubscriptionRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.entity.InsurancePolicyEntity
import com.expensetracker.app.core.database.entity.RecurringIncomeEntity
import com.expensetracker.app.core.database.entity.SavingsGoalEntity
import com.expensetracker.app.core.database.entity.SubscriptionEntity
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.ingestion.parser.SubscriptionMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SubscriptionUiModel(
    val entity: SubscriptionEntity,
    val name: String,
    val amount: Double,
    val billingCycle: String,
    val formattedDueDate: String,
    val statusText: String,
    val isOverdue: Boolean,
    val isDueSoon: Boolean,
    val category: String,
    val isActive: Boolean,
    val autoMatched: Boolean = false,
    val priceHike: PriceHikeAlert? = null
)

data class RecurringIncomeUiModel(
    val entity: RecurringIncomeEntity,
    val formattedDueDate: String,
    val statusText: String,
    val isOverdue: Boolean,
    val isReceivedThisCycle: Boolean
)

data class SubscriptionsUiState(
    val subscriptions: List<SubscriptionUiModel> = emptyList(),
    val detectedCandidates: List<DetectedSubscriptionCandidate> = emptyList(),
    val categories: List<String> = emptyList(),
    val totalMonthlyCost: Double = 0.0,
    val activeCount: Int = 0,
    val recurringIncomes: List<RecurringIncomeUiModel> = emptyList(),
    val totalExpectedMonthlyIncome: Double = 0.0,
    val totalReceivedMonthlyIncome: Double = 0.0,
    val provisioningPlan: SalaryProvisioningPlan = SalaryProvisioningPlan(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val selectedTab: Int = 0 // 0: Mandates, 1: Incomes, 2: Policies
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val subscriptionDetector: SubscriptionDetector,
    private val recurringIncomeRepository: RecurringIncomeRepository,
    private val insurancePolicyRepository: InsurancePolicyRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)

    val uiState: StateFlow<SubscriptionsUiState> = combine(
        combine(
            subscriptionRepository.allSubscriptions,
            categoryRepository.allCategories,
            transactionRepository.allTransactions
        ) { subs, categories, transactions ->
            Triple(subs, categories, transactions)
        },
        combine(
            recurringIncomeRepository.allIncomes,
            insurancePolicyRepository.allPolicies,
            savingsGoalRepository.allGoals,
            _selectedTab
        ) { incomes, policies, jars, tab ->
            Tuple4(incomes, policies, jars, tab)
        }
    ) { (subs, categories, transactions), (incomes, policies, jars, tab) ->
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val nowMs = System.currentTimeMillis()

        // 1. Process Mandates / Subscriptions
        val subModels = subs.map { entity ->
            val formattedDate = dateFormat.format(Date(entity.nextDueDateMs))
            val diffDays = ((entity.nextDueDateMs - nowMs) / (1000 * 60 * 60 * 24)).toInt()

            val (statusText, isOverdue, isDueSoon) = when {
                entity.autoMatched && entity.lastPaidDateMs != null && (nowMs - entity.lastPaidDateMs) < 7 * 86400000L -> {
                    Triple("Auto-Paid via SMS 🟢 (Next: $formattedDate)", false, false)
                }
                diffDays < 0 -> Triple("Overdue by ${-diffDays} day(s) ($formattedDate)", true, false)
                diffDays == 0 -> Triple("Due Today ($formattedDate)", false, true)
                diffDays <= 5 -> Triple("Due in $diffDays day(s) ($formattedDate)", false, true)
                else -> Triple("Next due: $formattedDate", false, false)
            }

            val priceHike = PriceCreepDetector.detectPriceHikeForSubscription(
                subscriptionName = entity.name,
                currentAmount = entity.amount,
                allTransactions = transactions
            )

            SubscriptionUiModel(
                entity = entity,
                name = entity.name,
                amount = entity.amount,
                billingCycle = entity.billingCycle,
                formattedDueDate = formattedDate,
                statusText = statusText,
                isOverdue = isOverdue,
                isDueSoon = isDueSoon,
                category = entity.category,
                isActive = entity.isActive,
                autoMatched = entity.autoMatched,
                priceHike = priceHike
            )
        }

        val totalMonthlyDebits = subs.filter { it.isActive }.sumOf { item ->
            when (item.billingCycle.lowercase()) {
                "yearly" -> item.amount / 12.0
                "weekly" -> item.amount * 4.33
                "quarterly" -> item.amount / 3.0
                else -> item.amount // Monthly
            }
        }

        // 2. Process Recurring Incomes
        val incomeModels = incomes.map { entity ->
            val formattedDate = dateFormat.format(Date(entity.nextDueDateMs))
            val diffDays = ((entity.nextDueDateMs - nowMs) / (1000 * 60 * 60 * 24)).toInt()
            val isReceivedRecently = entity.lastReceivedDateMs != null && (nowMs - entity.lastReceivedDateMs) < 30 * 86400000L

            val (statusText, isOverdue) = when {
                isReceivedRecently -> Pair("Received for cycle 🟢 (Next: $formattedDate)", false)
                diffDays < 0 -> Pair("Overdue by ${-diffDays} day(s)", true)
                diffDays == 0 -> Pair("Due Today ($formattedDate)", false)
                else -> Pair("Expected on $formattedDate", false)
            }

            RecurringIncomeUiModel(
                entity = entity,
                formattedDueDate = formattedDate,
                statusText = statusText,
                isOverdue = isOverdue,
                isReceivedThisCycle = isReceivedRecently
            )
        }

        val totalExpectedMonthlyInc = incomes.filter { it.isActive }.sumOf { item ->
            when (item.frequency.lowercase()) {
                "yearly", "annually" -> item.amount / 12.0
                "quarterly" -> item.amount / 3.0
                "semi-annually", "half-yearly" -> item.amount / 6.0
                "bi-weekly", "biweekly" -> item.amount * 2.16
                else -> item.amount // Monthly
            }
        }

        val totalReceivedMonthlyInc = incomes.filter { it.isActive && it.lastReceivedDateMs != null && (nowMs - it.lastReceivedDateMs) < 30 * 86400000L }
            .sumOf { it.amount }

        // 3. Process Insurance Policies & Salary Day Plan
        val plan = SalaryProvisioningCalculator.calculatePlan(policies, jars, nowMs)

        val candidates = subscriptionDetector.detectCandidates(transactions, subs)
        val catList = (listOf("Subscriptions", "Bills & Utilities", "Rent", "EMI", "Entertainment", "Health & Medical", "Investments", "General Expense", "Salary", "Rental Income", "Business") + categories.map { it.name }).distinct()

        SubscriptionsUiState(
            subscriptions = subModels,
            detectedCandidates = candidates,
            categories = catList,
            totalMonthlyCost = totalMonthlyDebits,
            activeCount = subs.count { it.isActive },
            recurringIncomes = incomeModels,
            totalExpectedMonthlyIncome = totalExpectedMonthlyInc,
            totalReceivedMonthlyIncome = totalReceivedMonthlyInc,
            provisioningPlan = plan,
            savingsGoals = jars,
            selectedTab = tab
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubscriptionsUiState()
    )

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    // --- Subscriptions / Mandates Actions ---
    fun addDetectedCandidate(candidate: DetectedSubscriptionCandidate) {
        saveSubscription(
            name = candidate.name,
            amount = candidate.amount,
            cycle = candidate.suggestedCycle,
            category = candidate.category,
            dueDateMs = System.currentTimeMillis() + (30L * 24 * 3600 * 1000)
        )
    }

    fun saveSubscription(name: String, amount: Double, cycle: String, category: String, dueDateMs: Long) {
        viewModelScope.launch {
            val entity = SubscriptionEntity(
                name = name,
                amount = amount,
                billingCycle = cycle,
                category = category,
                nextDueDateMs = dueDateMs,
                isActive = true
            )
            subscriptionRepository.saveSubscription(entity)
        }
    }

    fun updateSubscription(entity: SubscriptionEntity, name: String, amount: Double, cycle: String, category: String, dueDateMs: Long) {
        viewModelScope.launch {
            val updated = entity.copy(
                name = name,
                amount = amount,
                billingCycle = cycle,
                category = category,
                nextDueDateMs = dueDateMs
            )
            subscriptionRepository.updateSubscription(updated)
        }
    }

    fun markAsPaid(item: SubscriptionUiModel) {
        viewModelScope.launch {
            val nextDueDate = SubscriptionMatcher.calculateNextDueDate(item.entity.nextDueDateMs, item.billingCycle)
            val updated = item.entity.copy(
                nextDueDateMs = nextDueDate,
                lastPaidDateMs = System.currentTimeMillis(),
                autoMatched = true
            )
            subscriptionRepository.updateSubscription(updated)
        }
    }

    fun toggleSubscriptionActive(entity: SubscriptionEntity) {
        viewModelScope.launch {
            val updated = entity.copy(isActive = !entity.isActive)
            subscriptionRepository.updateSubscription(updated)
        }
    }

    fun deleteSubscription(entity: SubscriptionEntity) {
        viewModelScope.launch {
            subscriptionRepository.deleteSubscription(entity)
        }
    }

    // --- Recurring Inflow Actions ---
    fun saveRecurringIncome(
        name: String,
        amount: Double,
        frequency: String,
        dayOfMonth: Int,
        category: String,
        isSalary: Boolean,
        payer: String,
        dueDateMs: Long
    ) {
        viewModelScope.launch {
            val entity = RecurringIncomeEntity(
                name = name,
                amount = amount,
                frequency = frequency,
                expectedDayOfMonth = dayOfMonth,
                nextDueDateMs = dueDateMs,
                category = category,
                isSalary = isSalary,
                payerPpiOrBank = payer,
                isActive = true
            )
            recurringIncomeRepository.insertIncome(entity)
        }
    }

    fun updateRecurringIncome(
        entity: RecurringIncomeEntity,
        name: String,
        amount: Double,
        frequency: String,
        dayOfMonth: Int,
        category: String,
        isSalary: Boolean,
        payer: String,
        dueDateMs: Long
    ) {
        viewModelScope.launch {
            val updated = entity.copy(
                name = name,
                amount = amount,
                frequency = frequency,
                expectedDayOfMonth = dayOfMonth,
                nextDueDateMs = dueDateMs,
                category = category,
                isSalary = isSalary,
                payerPpiOrBank = payer
            )
            recurringIncomeRepository.updateIncome(updated)
        }
    }

    fun markIncomeReceived(income: RecurringIncomeEntity, recordInLedger: Boolean = true) {
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
                txId = transactionRepository.insertTransaction(tx)
            }
            recurringIncomeRepository.markReceived(income, txId)
        }
    }

    fun toggleIncomeActive(income: RecurringIncomeEntity) {
        viewModelScope.launch {
            recurringIncomeRepository.updateIncome(income.copy(isActive = !income.isActive))
        }
    }

    fun deleteIncome(income: RecurringIncomeEntity) {
        viewModelScope.launch {
            recurringIncomeRepository.deleteIncome(income)
        }
    }

    // --- Insurance Policy & Salary Stash Actions ---
    fun saveInsurancePolicy(
        policyName: String,
        policyType: String,
        insurerName: String,
        policyNumber: String,
        premiumAmount: Double,
        frequency: String,
        renewalDueDateMs: Long,
        taxSection: String,
        createLinkedJar: Boolean,
        jarColorHex: String = "#3B82F6"
    ) {
        viewModelScope.launch {
            val policy = InsurancePolicyEntity(
                policyName = policyName,
                policyType = policyType,
                insurerName = insurerName,
                policyNumber = policyNumber,
                premiumAmount = premiumAmount,
                frequency = frequency,
                renewalDueDateMs = renewalDueDateMs,
                taxSection = taxSection,
                isActive = true
            )
            insurancePolicyRepository.createPolicyWithSinkingJar(
                policy = policy,
                createJar = createLinkedJar,
                jarColorHex = jarColorHex
            )
        }
    }

    fun updateInsurancePolicy(
        entity: InsurancePolicyEntity,
        policyName: String,
        policyType: String,
        insurerName: String,
        policyNumber: String,
        premiumAmount: Double,
        frequency: String,
        renewalDueDateMs: Long,
        taxSection: String,
        linkedJarId: Long?
    ) {
        viewModelScope.launch {
            val updated = entity.copy(
                policyName = policyName,
                policyType = policyType,
                insurerName = insurerName,
                policyNumber = policyNumber,
                premiumAmount = premiumAmount,
                frequency = frequency,
                renewalDueDateMs = renewalDueDateMs,
                taxSection = taxSection,
                linkedSavingsGoalId = linkedJarId
            )
            insurancePolicyRepository.updatePolicy(updated)
        }
    }

    fun markPolicyRenewed(policy: InsurancePolicyEntity) {
        viewModelScope.launch {
            insurancePolicyRepository.markPolicyRenewed(policy)
        }
    }

    fun togglePolicyActive(policy: InsurancePolicyEntity) {
        viewModelScope.launch {
            insurancePolicyRepository.updatePolicy(policy.copy(isActive = !policy.isActive))
        }
    }

    fun deletePolicy(policy: InsurancePolicyEntity) {
        viewModelScope.launch {
            insurancePolicyRepository.deletePolicy(policy)
        }
    }

    fun stashToJar(jarId: Long, amount: Double, recordInLedger: Boolean = true) {
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
                transactionRepository.insertTransaction(tx)
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
