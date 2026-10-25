package com.expensetracker.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.core.database.entity.RecurringIncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringIncomeDao {
    @Query("SELECT * FROM recurring_incomes ORDER BY nextDueDateMs ASC")
    fun getAllIncomes(): Flow<List<RecurringIncomeEntity>>

    @Query("SELECT * FROM recurring_incomes ORDER BY nextDueDateMs ASC")
    suspend fun getAllIncomesList(): List<RecurringIncomeEntity>

    @Query("SELECT * FROM recurring_incomes WHERE id = :id LIMIT 1")
    suspend fun findIncomeById(id: Long): RecurringIncomeEntity?

    @Query("SELECT * FROM recurring_incomes WHERE isSalary = 1 AND isActive = 1 LIMIT 1")
    suspend fun getPrimarySalary(): RecurringIncomeEntity?

    @Query("SELECT * FROM recurring_incomes WHERE isSalary = 1 AND isActive = 1 LIMIT 1")
    fun getPrimarySalaryFlow(): Flow<RecurringIncomeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: RecurringIncomeEntity): Long

    @Update
    suspend fun updateIncome(income: RecurringIncomeEntity)

    @Delete
    suspend fun deleteIncome(income: RecurringIncomeEntity)
}
