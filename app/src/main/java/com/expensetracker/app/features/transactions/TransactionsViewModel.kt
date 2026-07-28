package com.expensetracker.app.features.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.CategoryRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.entity.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val monthYearOptions: List<String> = listOf("ALL"),
    val periodPresetOptions: List<String> = listOf("ALL", "THIS_MONTH", "LAST_MONTH", "LAST_3_MONTHS", "LAST_6_MONTHS", "YEARLY", "CUSTOM"),
    val selectedPeriodPreset: String = "ALL",
    val customStartDateMs: Long? = null,
    val customEndDateMs: Long? = null,
    val selectedMonthYear: String = "ALL",
    val selectedCategoryFilter: String = "ALL",
    val selectedFilter: String = "ALL", // ALL, DEBIT, CREDIT, INVESTMENT
    val selectedSortOrder: String = "NEWEST", // NEWEST, OLDEST, HIGH_LOW, LOW_HIGH
    val searchQuery: String = "",
    val lastDeletedTransaction: TransactionEntity? = null
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("ALL")
    private val _selectedCategoryFilter = MutableStateFlow("ALL")
    private val _selectedMonthYear = MutableStateFlow("ALL")
    private val _selectedPeriodPreset = MutableStateFlow("ALL")
    private val _customStartDateMs = MutableStateFlow<Long?>(null)
    private val _customEndDateMs = MutableStateFlow<Long?>(null)
    private val _selectedSortOrder = MutableStateFlow("NEWEST")
    private val _searchQuery = MutableStateFlow("")
    private val _lastDeletedTransaction = MutableStateFlow<TransactionEntity?>(null)

    private val _filterState = combine(
        combine(_selectedFilter, _selectedCategoryFilter, _selectedMonthYear) { f, c, m -> Triple(f, c, m) },
        combine(_selectedPeriodPreset, _customStartDateMs, _customEndDateMs) { p, s, e -> Triple(p, s, e) },
        combine(_selectedSortOrder, _searchQuery) { s, q -> Pair(s, q) }
    ) { fcm, pse, sq ->
        FilterState(
            filter = fcm.first,
            categoryFilter = fcm.second,
            monthYear = fcm.third,
            periodPreset = pse.first,
            customStartMs = pse.second,
            customEndMs = pse.third,
            sortOrder = sq.first,
            query = sq.second
        )
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.allTransactions,
        categoryRepository.allCategories,
        _filterState,
        _lastDeletedTransaction
    ) { list, categoryEntities, filterState, lastDeleted ->

        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val availableMonths = list
            .map { monthFormat.format(Date(it.timestamp)) }
            .distinct()
            .sortedDescending()
        val allMonthOptions = listOf("ALL") + availableMonths

        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH)

        val filtered = list.filter { item ->
            val matchesType = when (filterState.filter) {
                "DEBIT" -> item.type == "DEBIT"
                "CREDIT" -> item.type == "CREDIT"
                "INVESTMENT" -> item.type == "INVESTMENT"
                "TRANSFER" -> item.type == "TRANSFER"
                else -> true
            }
            val matchesCategory = if (filterState.categoryFilter == "ALL") true else item.category.equals(filterState.categoryFilter, ignoreCase = true)
            val matchesMonth = if (filterState.monthYear == "ALL") true else monthFormat.format(Date(item.timestamp)) == filterState.monthYear

            val itemCal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
            val itemYear = itemCal.get(Calendar.YEAR)
            val itemMonth = itemCal.get(Calendar.MONTH)

            val matchesPeriod = when (filterState.periodPreset) {
                "THIS_MONTH" -> itemYear == currentYear && itemMonth == currentMonth
                "LAST_MONTH" -> {
                    val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                    itemYear == lastMonthCal.get(Calendar.YEAR) && itemMonth == lastMonthCal.get(Calendar.MONTH)
                }
                "LAST_3_MONTHS" -> item.timestamp >= (System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000)
                "LAST_6_MONTHS" -> item.timestamp >= (System.currentTimeMillis() - 180L * 24 * 60 * 60 * 1000)
                "YEARLY" -> itemYear == currentYear
                "CUSTOM" -> {
                    val start = filterState.customStartMs ?: 0L
                    val end = filterState.customEndMs ?: Long.MAX_VALUE
                    item.timestamp in start..end
                }
                else -> true // ALL
            }

            val matchesQuery = filterState.query.isBlank() ||
                    item.merchant.contains(filterState.query, ignoreCase = true) ||
                    item.category.contains(filterState.query, ignoreCase = true)

            matchesType && matchesCategory && matchesMonth && matchesPeriod && matchesQuery
        }

        val sorted = when (filterState.sortOrder) {
            "OLDEST" -> filtered.sortedBy { it.timestamp }
            "HIGH_LOW" -> filtered.sortedByDescending { it.amount }
            "LOW_HIGH" -> filtered.sortedBy { it.amount }
            else -> filtered.sortedByDescending { it.timestamp }
        }

        val defaultCats = listOf("General Expense", "Food & Dining", "Transportation", "Shopping", "Bills & Utilities", "Rent", "EMI", "Groceries & Vegetables", "Subscriptions", "Entertainment", "Fuel", "Health & Medical", "Insurance", "Government & Fees", "Investments", "Income", "Transfer", "Other")
        val customCatNames = categoryEntities.map { it.name }
        val combinedCategories = (defaultCats + customCatNames).distinct()

        TransactionsUiState(
            transactions = sorted,
            categories = combinedCategories,
            monthYearOptions = allMonthOptions,
            selectedPeriodPreset = filterState.periodPreset,
            customStartDateMs = filterState.customStartMs,
            customEndDateMs = filterState.customEndMs,
            selectedMonthYear = filterState.monthYear,
            selectedCategoryFilter = filterState.categoryFilter,
            selectedFilter = filterState.filter,
            selectedSortOrder = filterState.sortOrder,
            searchQuery = filterState.query,
            lastDeletedTransaction = lastDeleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setMonthYearFilter(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    fun setPeriodPreset(preset: String, startMs: Long? = null, endMs: Long? = null) {
        _selectedPeriodPreset.value = preset
        _customStartDateMs.value = startMs
        _customEndDateMs.value = endMs
    }

    fun setSortOrder(sortOrder: String) {
        _selectedSortOrder.value = sortOrder
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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

    private data class FilterState(
        val filter: String,
        val categoryFilter: String,
        val monthYear: String,
        val periodPreset: String,
        val customStartMs: Long?,
        val customEndMs: Long?,
        val sortOrder: String,
        val query: String
    )
}
