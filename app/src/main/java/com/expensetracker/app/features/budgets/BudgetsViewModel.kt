package com.expensetracker.app.features.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.BudgetRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.entity.BudgetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryBudgetUiModel(
    val categoryName: String,
    val spentAmount: Double,
    val limitAmount: Double,
    val percentage: Float, // 0.0 to 1.0+
    val budgetId: Long = 0
)

data class BudgetsUiState(
    val budgetItems: List<CategoryBudgetUiModel> = emptyList(),
    val totalBudgetLimit: Double = 0.0,
    val totalSpent: Double = 0.0
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<BudgetsUiState> = combine(
        budgetRepository.allBudgets,
        transactionRepository.allTransactions
    ) { budgets, transactions ->
        val categorySpentMap = transactions
            .filter { it.type == "DEBIT" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val uiModels = budgets.map { budget ->
            val spent = categorySpentMap[budget.categoryName] ?: 0.0
            val pct = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat() else 0f
            CategoryBudgetUiModel(
                categoryName = budget.categoryName,
                spentAmount = spent,
                limitAmount = budget.limitAmount,
                percentage = pct,
                budgetId = budget.id
            )
        }

        val totalLimit = budgets.sumOf { it.limitAmount }
        val totalSpent = uiModels.sumOf { it.spentAmount }

        BudgetsUiState(
            budgetItems = uiModels,
            totalBudgetLimit = totalLimit,
            totalSpent = totalSpent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState()
    )

    fun saveBudget(categoryName: String, limitAmount: Double) {
        viewModelScope.launch {
            val entity = BudgetEntity(
                categoryName = categoryName,
                limitAmount = limitAmount
            )
            budgetRepository.saveBudget(entity)
        }
    }

    fun deleteBudget(categoryName: String, limitAmount: Double, id: Long) {
        viewModelScope.launch {
            val entity = BudgetEntity(id = id, categoryName = categoryName, limitAmount = limitAmount)
            budgetRepository.deleteBudget(entity)
        }
    }
}
