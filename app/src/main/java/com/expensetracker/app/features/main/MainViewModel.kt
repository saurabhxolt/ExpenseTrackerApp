package com.expensetracker.app.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.promotions.Promotion
import com.expensetracker.app.core.promotions.PromotionManager
import com.expensetracker.app.core.security.PrivacyModeManager
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.entity.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val promotionManager: PromotionManager,
    private val privacyModeManager: PrivacyModeManager,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _promotions = MutableStateFlow<List<Promotion>>(emptyList())
    val promotions: StateFlow<List<Promotion>> = _promotions

    val isPrivacyMode: StateFlow<Boolean> = privacyModeManager.isPrivacyMode

    private val _showQuickCashLog = MutableStateFlow(false)
    val showQuickCashLog: StateFlow<Boolean> = _showQuickCashLog

    init {
        loadPromotions()
    }

    fun loadPromotions() {
        viewModelScope.launch {
            _promotions.value = promotionManager.getActivePromotions()
        }
    }

    fun togglePrivacyMode() {
        privacyModeManager.toggle()
    }

    fun openQuickCashLog() {
        _showQuickCashLog.value = true
    }

    fun closeQuickCashLog() {
        _showQuickCashLog.value = false
    }

    fun saveCashSpend(amount: Double, category: String, merchant: String, note: String) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                amount = amount,
                type = "DEBIT",
                merchant = if (merchant.isBlank()) "Cash Spend" else merchant,
                rawText = "Cash spend: $note",
                category = category,
                timestamp = System.currentTimeMillis(),
                note = note,
                isManual = true,
                transactionHash = "cash_${System.currentTimeMillis()}_${(1000..9999).random()}"
            )
            transactionRepository.insertTransaction(tx)
        }
    }
}
