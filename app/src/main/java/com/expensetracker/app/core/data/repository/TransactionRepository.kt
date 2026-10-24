package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.TransactionDao
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.ingestion.parser.SubscriptionMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val subscriptionMatcher: SubscriptionMatcher
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions().map { list ->
        list.map { item ->
            if ((item.category.equals("Investments", ignoreCase = true) || item.category.equals("Investment", ignoreCase = true)) && item.type != "INVESTMENT") {
                item.copy(type = "INVESTMENT")
            } else {
                item
            }
        }
    }
    val totalExpenses: Flow<Double?> = transactionDao.getTotalExpenses()
    val totalIncome: Flow<Double?> = transactionDao.getTotalIncome()

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val sanitized = if (transaction.category.equals("Investments", ignoreCase = true) || transaction.category.equals("Investment", ignoreCase = true)) {
            transaction.copy(type = "INVESTMENT")
        } else {
            transaction
        }
        val insertedId = transactionDao.insertTransaction(sanitized)
        if (insertedId > 0) {
            subscriptionMatcher.matchAndAdvance(sanitized.copy(id = insertedId))
        }
        return insertedId
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        val sanitized = if (transaction.category.equals("Investments", ignoreCase = true) || transaction.category.equals("Investment", ignoreCase = true)) {
            transaction.copy(type = "INVESTMENT")
        } else {
            transaction
        }
        transactionDao.updateTransaction(sanitized)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }
}
