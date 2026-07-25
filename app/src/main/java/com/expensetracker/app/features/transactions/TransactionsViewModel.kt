package com.expensetracker.app.features.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.entity.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val selectedFilter: String = "ALL", // ALL, DEBIT, CREDIT
    val searchQuery: String = ""
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("ALL")
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.allTransactions,
        _selectedFilter,
        _searchQuery
    ) { list, filter, query ->
        val filtered = list.filter { item ->
            val matchesType = when (filter) {
                "DEBIT" -> item.type == "DEBIT"
                "CREDIT" -> item.type == "CREDIT"
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                    item.merchant.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true)

            matchesType && matchesQuery
        }

        TransactionsUiState(
            transactions = filtered,
            selectedFilter = filter,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
