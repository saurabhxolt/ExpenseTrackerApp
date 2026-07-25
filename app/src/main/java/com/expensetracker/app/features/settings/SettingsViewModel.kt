package com.expensetracker.app.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.BudgetRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.dao.CategoryDao
import com.expensetracker.app.features.backup.BackupManager
import com.expensetracker.app.features.reports.ReportsExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val transactionCount: Int = 0,
    val budgetCount: Int = 0,
    val isBiometricEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _isBiometricEnabled = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        transactionRepository.allTransactions,
        budgetRepository.allBudgets,
        _isBiometricEnabled
    ) { transactions, budgets, biometric ->
        SettingsUiState(
            transactionCount = transactions.size,
            budgetCount = budgets.size,
            isBiometricEnabled = biometric
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun toggleBiometric(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
    }

    suspend fun exportCsvData(): String {
        val transactions = transactionRepository.allTransactions.first()
        return ReportsExporter.generateCsvReport(transactions)
    }

    suspend fun exportBackupJson(): String {
        val transactions = transactionRepository.allTransactions.first()
        val budgets = budgetRepository.allBudgets.first()
        val categories = categoryDao.getAllCategories().first()
        return BackupManager.createBackupJson(transactions, categories, budgets)
    }

    fun restoreBackupJson(jsonStr: String) {
        viewModelScope.launch {
            val (transactions, categories, budgets) = BackupManager.parseBackupJson(jsonStr)
            for (t in transactions) {
                transactionRepository.insertTransaction(t)
            }
            for (b in budgets) {
                budgetRepository.saveBudget(b)
            }
            categoryDao.insertCategories(categories)
        }
    }
}
