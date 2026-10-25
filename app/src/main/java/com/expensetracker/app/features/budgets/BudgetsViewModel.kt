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
    val totalSpent: Double = 0.0,
    val availableCategories: List<String> = emptyList()
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: com.expensetracker.app.core.data.repository.CategoryRepository
) : ViewModel() {

    val uiState: StateFlow<BudgetsUiState> = combine(
        budgetRepository.allBudgets,
        transactionRepository.allTransactions,
        categoryRepository.allCategories
    ) { budgets, transactions, categoryEntities ->
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val currentMonthStart = cal.timeInMillis
        cal.add(java.util.Calendar.MONTH, 1)
        val currentMonthEnd = cal.timeInMillis - 1

        val categorySpentMap = transactions
            .filter { it.type == "DEBIT" && it.timestamp in currentMonthStart..currentMonthEnd }
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

        val defaultCats = listOf("General Expense", "Food & Dining", "Transportation", "Shopping", "Bills & Utilities", "Rent", "EMI", "Groceries & Vegetables", "Subscriptions", "Entertainment", "Fuel", "Health & Medical", "Insurance", "Government & Fees", "Investments", "Other")
        val customCatNames = categoryEntities.filter { it.type == "EXPENSE" || it.type == "INVESTMENT" }.map { it.name }
        val allCategories = (defaultCats + customCatNames).distinct()

        BudgetsUiState(
            budgetItems = uiModels,
            totalBudgetLimit = totalLimit,
            totalSpent = totalSpent,
            availableCategories = allCategories
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
