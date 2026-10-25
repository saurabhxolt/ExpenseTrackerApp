package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.SavingsGoalDao
import com.expensetracker.app.core.database.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsGoalRepository @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao
) {
    val allGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals()

    suspend fun getGoalById(id: Long): SavingsGoalEntity? {
        return savingsGoalDao.getGoalById(id)
    }

    suspend fun createGoal(goal: SavingsGoalEntity): Long {
        return savingsGoalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: SavingsGoalEntity) {
        savingsGoalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: SavingsGoalEntity) {
        savingsGoalDao.deleteGoal(goal)
    }

    suspend fun depositAmount(id: Long, amount: Double) {
        if (amount > 0) {
            savingsGoalDao.updateAmount(id, amount)
            val updated = savingsGoalDao.getGoalById(id)
            if (updated != null && updated.currentAmount >= updated.targetAmount && !updated.isCompleted) {
                savingsGoalDao.setCompleted(id, true)
            }
        }
    }

    suspend fun withdrawAmount(id: Long, amount: Double) {
        if (amount > 0) {
            savingsGoalDao.updateAmount(id, -amount)
            val updated = savingsGoalDao.getGoalById(id)
            if (updated != null && updated.currentAmount < updated.targetAmount && updated.isCompleted) {
                savingsGoalDao.setCompleted(id, false)
            }
        }
    }

    suspend fun toggleComplete(id: Long, isCompleted: Boolean) {
        savingsGoalDao.setCompleted(id, isCompleted)
    }
}
