package com.expensetracker.app.features.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.BudgetRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.database.dao.CategoryDao
import com.expensetracker.app.core.promotions.Announcement
import com.expensetracker.app.core.promotions.PromotionManager
import com.expensetracker.app.features.backup.BackupManager
import com.expensetracker.app.features.reports.ReportsExporter
import com.expensetracker.app.features.security.BiometricLockManager
import com.expensetracker.app.core.database.dao.MerchantCategoryMappingDao
import com.expensetracker.app.core.database.dao.SubscriptionDao
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
    val isBiometricEnabled: Boolean = false,
    val isDailyReviewEnabled: Boolean = true,
    val dailyReviewHour: Int = 21,
    val dailyReviewMinute: Int = 0,
    val announcements: List<Announcement> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryDao: CategoryDao,
    private val subscriptionDao: SubscriptionDao,
    private val merchantCategoryMappingDao: MerchantCategoryMappingDao,
    private val promotionManager: PromotionManager
) : ViewModel() {

    private val _isBiometricEnabled = MutableStateFlow(false)
    private val _isDailyReviewEnabled = MutableStateFlow(true)
    private val _dailyReviewHour = MutableStateFlow(21)
    private val _dailyReviewMinute = MutableStateFlow(0)
    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(transactionRepository.allTransactions, budgetRepository.allBudgets) { t, b -> Pair(t, b) },
        combine(_isBiometricEnabled, _isDailyReviewEnabled, _dailyReviewHour, _dailyReviewMinute) { bio, dr, h, m -> Quad(bio, dr, h, m) },
        _announcements
    ) { tb, settings, announcementsList ->
        SettingsUiState(
            transactionCount = tb.first.size,
            budgetCount = tb.second.size,
            isBiometricEnabled = settings.bio,
            isDailyReviewEnabled = settings.dr,
            dailyReviewHour = settings.h,
            dailyReviewMinute = settings.m,
            announcements = announcementsList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun loadInitialState(context: Context) {
        _isBiometricEnabled.value = BiometricLockManager.isBiometricLockEnabled(context)
        _isDailyReviewEnabled.value = com.expensetracker.app.ingestion.worker.DailyReviewWorker.isDailyReviewEnabled(context)
        val (h, m) = com.expensetracker.app.ingestion.worker.DailyReviewWorker.getDailyReviewTime(context)
        _dailyReviewHour.value = h
        _dailyReviewMinute.value = m
        viewModelScope.launch {
            _announcements.value = promotionManager.getActiveAnnouncements()
        }
    }

    fun toggleBiometric(context: Context, enabled: Boolean) {
        BiometricLockManager.setBiometricLockEnabled(context, enabled)
        _isBiometricEnabled.value = enabled
    }

    fun toggleDailyReview(context: Context, enabled: Boolean) {
        com.expensetracker.app.ingestion.worker.DailyReviewWorker.setDailyReviewPreferences(
            context, enabled, _dailyReviewHour.value, _dailyReviewMinute.value
        )
        _isDailyReviewEnabled.value = enabled
    }

    fun setDailyReviewTime(context: Context, hour: Int, minute: Int) {
        com.expensetracker.app.ingestion.worker.DailyReviewWorker.setDailyReviewPreferences(
            context, _isDailyReviewEnabled.value, hour, minute
        )
        _dailyReviewHour.value = hour
        _dailyReviewMinute.value = minute
    }

    suspend fun exportCsvData(): String {
        val transactions = transactionRepository.allTransactions.first()
        return ReportsExporter.generateCsvReport(transactions)
    }

    suspend fun exportBackupJson(): String {
        val transactions = transactionRepository.allTransactions.first()
        val budgets = budgetRepository.allBudgets.first()
        val categories = categoryDao.getAllCategories().first()
        val subscriptions = subscriptionDao.getAllSubscriptionsList()
        val merchantMappings = merchantCategoryMappingDao.getAllMappingsList()
        return BackupManager.createBackupJson(
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            subscriptions = subscriptions,
            merchantMappings = merchantMappings
        )
    }

    fun restoreBackupJson(jsonStr: String) {
        viewModelScope.launch {
            val data = BackupManager.parseBackupData(jsonStr)
            for (t in data.transactions) {
                transactionRepository.insertTransaction(t)
            }
            for (b in data.budgets) {
                budgetRepository.saveBudget(b)
            }
            categoryDao.insertCategories(data.categories)
            for (s in data.subscriptions) {
                subscriptionDao.insertSubscription(s)
            }
            for (m in data.merchantMappings) {
                merchantCategoryMappingDao.insertMapping(m)
            }
        }
    }
}

private data class Quad<A, B, C, D>(val bio: A, val dr: B, val h: C, val m: D)
