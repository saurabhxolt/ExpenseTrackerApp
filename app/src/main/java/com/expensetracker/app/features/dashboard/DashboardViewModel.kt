package com.expensetracker.app.features.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.CategoryRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.core.utils.PermissionUtils
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
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DashboardUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val monthYearOptions: List<String> = listOf("ALL"),
    val selectedMonthYear: String = "ALL",
    val selectedSortOrder: String = "NEWEST", // NEWEST, OLDEST, HIGH_LOW, LOW_HIGH
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0,
    val isAutoTrackingEnabled: Boolean = false,
    val isScanning: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    private val _isAutoTrackingEnabled = MutableStateFlow(false)
    private val _selectedMonthYear = MutableStateFlow("ALL")
    private val _selectedSortOrder = MutableStateFlow("NEWEST")

    private val _filterState = combine(
        _selectedMonthYear,
        _selectedSortOrder,
        _isAutoTrackingEnabled,
        _isScanning
    ) { monthYear, sortOrder, autoTrack, scanning ->
        FilterState(monthYear, sortOrder, autoTrack, scanning)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allTransactions,
        categoryRepository.allCategories,
        _filterState
    ) { transactions, categoryEntities, filterState ->

        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val availableMonths = transactions
            .map { monthFormat.format(Date(it.timestamp)) }
            .distinct()
            .sortedDescending()
        val allMonthOptions = listOf("ALL") + availableMonths

        val monthFiltered = if (filterState.monthYear == "ALL") {
            transactions
        } else {
            transactions.filter { monthFormat.format(Date(it.timestamp)) == filterState.monthYear }
        }

        val sortedTransactions = when (filterState.sortOrder) {
            "OLDEST" -> monthFiltered.sortedBy { it.timestamp }
            "HIGH_LOW" -> monthFiltered.sortedByDescending { it.amount }
            "LOW_HIGH" -> monthFiltered.sortedBy { it.amount }
            else -> monthFiltered.sortedByDescending { it.timestamp }
        }

        val totalExp = monthFiltered.filter { it.type == "DEBIT" }.sumOf { it.amount }
        val totalInc = monthFiltered.filter { it.type == "CREDIT" }.sumOf { it.amount }

        val defaultCats = listOf("Food & Dining", "Transportation", "Shopping", "Bills & Utilities", "Investments", "Income", "Other")
        val customCatNames = categoryEntities.map { it.name }
        val combinedCategories = (defaultCats + customCatNames).distinct()

        DashboardUiState(
            transactions = sortedTransactions,
            categories = combinedCategories,
            monthYearOptions = allMonthOptions,
            selectedMonthYear = filterState.monthYear,
            selectedSortOrder = filterState.sortOrder,
            totalExpenses = totalExp,
            totalIncome = totalInc,
            netBalance = totalInc - totalExp,
            isAutoTrackingEnabled = filterState.isAutoTrackingEnabled,
            isScanning = filterState.isScanning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun refreshPermissionState(context: Context) {
        val fullyEnabled = PermissionUtils.isAutoTrackingFullyEnabled(context)
        _isAutoTrackingEnabled.value = fullyEnabled

        if (PermissionUtils.isSmsPermissionGranted(context)) {
            triggerSmsInboxScan(context)
        }
    }

    fun setMonthYearFilter(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    fun setSortOrder(sortOrder: String) {
        _selectedSortOrder.value = sortOrder
    }

    fun triggerSmsInboxScan(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                SmsScanner.scanInbox(context)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun addTransaction(merchant: String, amount: Double, type: String, category: String, note: String = "") {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val hash = "$merchant-$amount-$timestamp-${(0..9999).random()}"
            val entity = TransactionEntity(
                amount = amount,
                type = type,
                merchant = merchant,
                rawText = "Manual Entry: $merchant ₹$amount",
                category = category,
                timestamp = timestamp,
                note = note,
                transactionHash = hash,
                isManual = true
            )
            repository.insertTransaction(entity)
        }
    }

    fun updateTransaction(transaction: TransactionEntity, newMerchant: String, newCategory: String, newAmount: Double) {
        viewModelScope.launch {
            val updated = transaction.copy(
                merchant = newMerchant,
                category = newCategory,
                amount = newAmount
            )
            repository.updateTransaction(updated)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    private data class FilterState(
        val monthYear: String,
        val sortOrder: String,
        val isAutoTrackingEnabled: Boolean,
        val isScanning: Boolean
    )
}
