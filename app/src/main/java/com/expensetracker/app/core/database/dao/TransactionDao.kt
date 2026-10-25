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

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT'")
    fun getTotalExpenses(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT'")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT (COALESCE((SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT'), 0.0) - COALESCE((SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT'), 0.0))")
    suspend fun getNetBalance(): Double

    @Query("SELECT (COALESCE((SELECT SUM(amount) FROM transactions WHERE type = 'CREDIT' AND timestamp >= :startTimestamp), 0.0) - COALESCE((SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND timestamp >= :startTimestamp), 0.0))")
    suspend fun getNetBalanceSince(startTimestamp: Long): Double

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEBIT' AND timestamp >= :startOfDayTimestamp")
    suspend fun getTodaySpend(startOfDayTimestamp: Long): Double?

    @Query("SELECT * FROM transactions WHERE type = :type AND ABS(amount - :amount) < 0.01 LIMIT 1")
    suspend fun findCandidateDuplicate(type: String, amount: Double): TransactionEntity?

    @Query("""
        SELECT * FROM transactions 
        WHERE type = :type 
          AND ABS(amount - :amount) < 0.01 
          AND ABS(timestamp - :timestamp) <= :windowMs
        ORDER BY ABS(timestamp - :timestamp) ASC
    """)
    suspend fun findDuplicatesInWindow(type: String, amount: Double, timestamp: Long, windowMs: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE ABS(amount - :amount) < 0.01 AND (merchant LIKE '%Credit Card%' OR merchant LIKE '%Card Bill%' OR merchant LIKE '%CRED%' OR merchant LIKE '%CheQ%' OR type = 'TRANSFER' OR category = 'Bills & Utilities' OR rawText LIKE '%credit card%' OR rawText LIKE '%sbi card%' OR rawText LIKE '%bobcard%' OR rawText LIKE '%onecard%' OR rawText LIKE '%cheq%' OR rawText LIKE '%cred%' OR rawText LIKE '%billdesk%' OR rawText LIKE '%bbps%' OR rawText LIKE '%ccpay%' OR rawText LIKE '%cardbill%') LIMIT 1")
    suspend fun findExistingCreditCardPayment(amount: Double): TransactionEntity?

    @Query("""
        SELECT * FROM transactions 
        WHERE ABS(amount - :amount) < 0.01 
          AND ABS(timestamp - :timestamp) <= :windowMs
          AND (merchant LIKE '%Credit Card%' OR merchant LIKE '%Card Bill%' OR merchant LIKE '%CRED%' OR merchant LIKE '%CheQ%' OR type = 'TRANSFER' OR category = 'Bills & Utilities' OR rawText LIKE '%credit card%' OR rawText LIKE '%sbi card%' OR rawText LIKE '%bobcard%' OR rawText LIKE '%onecard%' OR rawText LIKE '%cheq%' OR rawText LIKE '%cred%' OR rawText LIKE '%billdesk%' OR rawText LIKE '%bbps%' OR rawText LIKE '%ccpay%' OR rawText LIKE '%cardbill%')
        ORDER BY ABS(timestamp - :timestamp) ASC
    """)
    suspend fun findCreditCardPaymentsInWindow(amount: Double, timestamp: Long, windowMs: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET category = :newCategoryName WHERE category = :oldCategoryName")
    suspend fun updateTransactionCategoryName(oldCategoryName: String, newCategoryName: String)

    @Query("UPDATE transactions SET taxDeductibleSection = :section WHERE id = :id")
    suspend fun updateTaxSection(id: Long, section: String?)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
}
