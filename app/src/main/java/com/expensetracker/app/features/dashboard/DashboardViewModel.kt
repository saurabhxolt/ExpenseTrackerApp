package com.expensetracker.app.features.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.core.utils.PermissionUtils
import com.expensetracker.app.ingestion.sms.SmsScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val totalExpenses: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0,
    val isAutoTrackingEnabled: Boolean = false,
    val isScanning: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    private val _isAutoTrackingEnabled = MutableStateFlow(false)

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.allTransactions,
        repository.totalExpenses,
        repository.totalIncome,
        _isAutoTrackingEnabled,
        _isScanning
    ) { transactions, expenses, income, autoTrack, scanning ->
        val totalExp = expenses ?: 0.0
        val totalInc = income ?: 0.0
        DashboardUiState(
            transactions = transactions,
            totalExpenses = totalExp,
            totalIncome = totalInc,
            netBalance = totalInc - totalExp,
            isAutoTrackingEnabled = autoTrack,
            isScanning = scanning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun refreshPermissionState(context: Context) {
        val fullyEnabled = PermissionUtils.isAutoTrackingFullyEnabled(context)
        _isAutoTrackingEnabled.value = fullyEnabled

        // If SMS permission granted, automatically scan inbox for current month / incremental transactions
        if (PermissionUtils.isSmsPermissionGranted(context)) {
            triggerSmsInboxScan(context)
        }
    }

    fun triggerSmsInboxScan(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                SmsScanner.scanInbox(context)
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun addTransaction(merchant: String, amount: Double, type: String, category: String, note: String = "") {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val hash = "$merchant-$amount-$timestamp-${(0..9999).random()}"
            val entity = TransactionEntity(
                amount = amount,
                type = type,
                merchant = merchant,
                rawText = "Manual Entry: $merchant ₹$amount",
                category = category,
                timestamp = timestamp,
                note = note,
                transactionHash = hash,
                isManual = true
            )
            repository.insertTransaction(entity)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
