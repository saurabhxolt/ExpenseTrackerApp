package com.expensetracker.app.features.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CategoryShare(
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float
)

data class AnalyticsUiState(
    val categoryShares: List<CategoryShare> = emptyList(),
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val topSpendingCategory: String = "None"
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = transactionRepository.allTransactions.map { transactions ->
        val debitTransactions = transactions.filter { it.type == "DEBIT" }
        val creditTransactions = transactions.filter { it.type == "CREDIT" }

        val totalExp = debitTransactions.sumOf { it.amount }
        val totalInc = creditTransactions.sumOf { it.amount }

        val shares = debitTransactions
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .map { (cat, amount) ->
                val pct = if (totalExp > 0) (amount / totalExp).toFloat() else 0f
                CategoryShare(categoryName = cat, totalAmount = amount, percentage = pct)
            }
            .sortedByDescending { it.totalAmount }

        val topCategory = shares.firstOrNull()?.categoryName ?: "None"

        AnalyticsUiState(
            categoryShares = shares,
            totalExpenses = totalExp,
            totalIncome = totalInc,
            topSpendingCategory = topCategory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )
}
