package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.RecurringIncomeDao
import com.expensetracker.app.core.database.entity.RecurringIncomeEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringIncomeRepository @Inject constructor(
    private val recurringIncomeDao: RecurringIncomeDao
) {
    val allIncomes: Flow<List<RecurringIncomeEntity>> = recurringIncomeDao.getAllIncomes()
    val primarySalary: Flow<RecurringIncomeEntity?> = recurringIncomeDao.getPrimarySalaryFlow()

    suspend fun getAllIncomesList(): List<RecurringIncomeEntity> = recurringIncomeDao.getAllIncomesList()

    suspend fun getPrimarySalary(): RecurringIncomeEntity? = recurringIncomeDao.getPrimarySalary()

    suspend fun insertIncome(income: RecurringIncomeEntity): Long = recurringIncomeDao.insertIncome(income)

    suspend fun updateIncome(income: RecurringIncomeEntity) = recurringIncomeDao.updateIncome(income)

    suspend fun deleteIncome(income: RecurringIncomeEntity) = recurringIncomeDao.deleteIncome(income)

    suspend fun markReceived(income: RecurringIncomeEntity, transactionId: Long? = null) {
        val nextDue = calculateNextDueDate(income.nextDueDateMs, income.frequency)
        val updated = income.copy(
            lastReceivedDateMs = System.currentTimeMillis(),
            matchedTransactionId = transactionId ?: income.matchedTransactionId,
            nextDueDateMs = nextDue
        )
        recurringIncomeDao.updateIncome(updated)
    }

    companion object {
        fun calculateNextDueDate(currentDueDateMs: Long, frequency: String): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = if (currentDueDateMs > 0) currentDueDateMs else System.currentTimeMillis()
            when (frequency.lowercase()) {
                "bi-weekly", "biweekly", "fortnightly" -> cal.add(Calendar.DAY_OF_YEAR, 14)
                "monthly" -> cal.add(Calendar.MONTH, 1)
                "quarterly" -> cal.add(Calendar.MONTH, 3)
                "semi-annually", "half-yearly" -> cal.add(Calendar.MONTH, 6)
                "yearly", "annually" -> cal.add(Calendar.YEAR, 1)
                else -> cal.add(Calendar.MONTH, 1)
            }
            return cal.timeInMillis
        }
    }
}
