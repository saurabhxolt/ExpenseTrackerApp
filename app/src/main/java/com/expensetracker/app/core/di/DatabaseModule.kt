package com.expensetracker.app.core.di

import android.content.Context
import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.database.dao.BudgetDao
import com.expensetracker.app.core.database.dao.CategoryDao
import com.expensetracker.app.core.database.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExpenseDatabase {
        // Passphrase for SQLCipher Room DB encryption
        val passphrase = "expense_tracker_secret_passphrase_key".toByteArray()
        return ExpenseDatabase.getInstance(context, passphrase)
    }

    @Provides
    fun provideTransactionDao(db: ExpenseDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: ExpenseDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideBudgetDao(db: ExpenseDatabase): BudgetDao = db.budgetDao()
}
