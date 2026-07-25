package com.expensetracker.app.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.entity.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allTransactions,
        repository.totalExpenses,
        repository.totalIncome
    ) { transactions, expenses, income ->
        val totalExp = expenses ?: 0.0
        val totalInc = income ?: 0.0
        DashboardUiState(
            transactions = transactions,
            totalExpenses = totalExp,
            totalIncome = totalInc,
            netBalance = totalInc - totalExp
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

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

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
