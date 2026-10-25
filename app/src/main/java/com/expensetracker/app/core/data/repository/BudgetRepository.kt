package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.BudgetDao
import com.expensetracker.app.core.database.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun getBudgetByCategory(categoryName: String): BudgetEntity? {
        return budgetDao.getBudgetByCategory(categoryName)
    }

    suspend fun saveBudget(budget: BudgetEntity) {
        val existing = budgetDao.getBudgetByCategory(budget.categoryName)
        if (existing != null) {
            budgetDao.updateBudget(existing.copy(limitAmount = budget.limitAmount, period = budget.period))
        } else {
            budgetDao.insertBudget(budget)
        }
    }

    suspend fun updateBudget(budget: BudgetEntity) {
        budgetDao.updateBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        budgetDao.deleteBudget(budget)
    }
}
