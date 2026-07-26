package com.expensetracker.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT'")
    fun getTotalExpenses(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT'")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE type = :type AND ABS(amount - :amount) < 0.01 AND timestamp >= :minTimestamp AND timestamp <= :maxTimestamp LIMIT 1")
    suspend fun findCandidateDuplicate(type: String, amount: Double, minTimestamp: Long, maxTimestamp: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE ABS(amount - :amount) < 0.01 AND (merchant LIKE '%Credit Card%' OR category = 'Bills & Utilities' OR rawText LIKE '%credit card%') LIMIT 1")
    suspend fun findExistingCreditCardPayment(amount: Double): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
}
