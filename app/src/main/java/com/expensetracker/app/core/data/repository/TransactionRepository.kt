package com.expensetracker.app.core.data.repository

import android.content.Context
import com.expensetracker.app.core.database.dao.TransactionDao
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.ingestion.parser.SubscriptionMatcher
import com.expensetracker.app.widget.ExpenseWidgetProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val subscriptionMatcher: SubscriptionMatcher,
    @ApplicationContext private val context: Context
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
            ExpenseWidgetProvider.updateAllWidgets(context)
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
        ExpenseWidgetProvider.updateAllWidgets(context)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
        ExpenseWidgetProvider.updateAllWidgets(context)
    }
}
