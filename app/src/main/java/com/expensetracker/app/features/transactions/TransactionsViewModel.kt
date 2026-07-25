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
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<String> = emptyList(),
    val monthYearOptions: List<String> = listOf("ALL"),
    val selectedMonthYear: String = "ALL",
    val selectedFilter: String = "ALL", // ALL, DEBIT, CREDIT
    val selectedSortOrder: String = "NEWEST", // NEWEST, OLDEST, HIGH_LOW, LOW_HIGH
    val searchQuery: String = ""
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("ALL")
    private val _selectedMonthYear = MutableStateFlow("ALL")
    private val _selectedSortOrder = MutableStateFlow("NEWEST")
    private val _searchQuery = MutableStateFlow("")

    private val _filterState = combine(
        _selectedFilter,
        _selectedMonthYear,
        _selectedSortOrder,
        _searchQuery
    ) { filter, monthYear, sortOrder, query ->
        FilterState(filter, monthYear, sortOrder, query)
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.allTransactions,
        categoryRepository.allCategories,
        _filterState
    ) { list, categoryEntities, filterState ->

        val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val availableMonths = list
            .map { monthFormat.format(Date(it.timestamp)) }
            .distinct()
            .sortedDescending()
        val allMonthOptions = listOf("ALL") + availableMonths

        val filtered = list.filter { item ->
            val matchesType = when (filterState.filter) {
                "DEBIT" -> item.type == "DEBIT"
                "CREDIT" -> item.type == "CREDIT"
                else -> true
            }
            val matchesMonth = if (filterState.monthYear == "ALL") true else monthFormat.format(Date(item.timestamp)) == filterState.monthYear
            val matchesQuery = filterState.query.isBlank() ||
                    item.merchant.contains(filterState.query, ignoreCase = true) ||
                    item.category.contains(filterState.query, ignoreCase = true)

            matchesType && matchesMonth && matchesQuery
        }

        val sorted = when (filterState.sortOrder) {
            "OLDEST" -> filtered.sortedBy { it.timestamp }
            "HIGH_LOW" -> filtered.sortedByDescending { it.amount }
            "LOW_HIGH" -> filtered.sortedBy { it.amount }
            else -> filtered.sortedByDescending { it.timestamp }
        }

        val defaultCats = listOf("Food & Dining", "Transportation", "Shopping", "Bills & Utilities", "Investments", "Income", "Other")
        val customCatNames = categoryEntities.map { it.name }
        val combinedCategories = (defaultCats + customCatNames).distinct()

        TransactionsUiState(
            transactions = sorted,
            categories = combinedCategories,
            monthYearOptions = allMonthOptions,
            selectedMonthYear = filterState.monthYear,
            selectedFilter = filterState.filter,
            selectedSortOrder = filterState.sortOrder,
            searchQuery = filterState.query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setMonthYearFilter(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    fun setSortOrder(sortOrder: String) {
        _selectedSortOrder.value = sortOrder
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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
        val filter: String,
        val monthYear: String,
        val sortOrder: String,
        val query: String
    )
}
