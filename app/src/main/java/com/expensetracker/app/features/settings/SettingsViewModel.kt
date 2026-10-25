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

import java.io.File
import com.expensetracker.app.ingestion.worker.ScheduledBackupWorker

data class SettingsUiState(
    val transactionCount: Int = 0,
    val budgetCount: Int = 0,
    val isBiometricEnabled: Boolean = false,
    val isDailyReviewEnabled: Boolean = true,
    val dailyReviewHour: Int = 21,
    val dailyReviewMinute: Int = 0,
    val isShakeToHideEnabled: Boolean = true,
    val isDefaultPrivacyModeEnabled: Boolean = false,
    val isAutoBackupEnabled: Boolean = true,
    val lastAutoBackupTime: Long = 0L,
    val availableAutoBackups: List<File> = emptyList(),
    val announcements: List<Announcement> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryDao: CategoryDao,
    private val subscriptionDao: SubscriptionDao,
    private val merchantCategoryMappingDao: MerchantCategoryMappingDao,
    private val splitDao: com.expensetracker.app.core.database.dao.SplitDao,
    private val promotionManager: PromotionManager,
    private val privacyModeManager: com.expensetracker.app.core.security.PrivacyModeManager
) : ViewModel() {

    private val _isBiometricEnabled = MutableStateFlow(false)
    private val _isDailyReviewEnabled = MutableStateFlow(true)
    private val _dailyReviewHour = MutableStateFlow(21)
    private val _dailyReviewMinute = MutableStateFlow(0)
    private val _isShakeToHideEnabled = MutableStateFlow(true)
    private val _isDefaultPrivacyModeEnabled = MutableStateFlow(false)
    private val _isAutoBackupEnabled = MutableStateFlow(true)
    private val _lastAutoBackupTime = MutableStateFlow(0L)
    private val _availableAutoBackups = MutableStateFlow<List<File>>(emptyList())
    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(transactionRepository.allTransactions, budgetRepository.allBudgets) { t, b -> Pair(t, b) },
        combine(_isBiometricEnabled, _isDailyReviewEnabled, _dailyReviewHour, _dailyReviewMinute) { bio, dr, h, m -> Quad(bio, dr, h, m) },
        combine(_isShakeToHideEnabled, _isDefaultPrivacyModeEnabled) { s, d -> Pair(s, d) },
        combine(_isAutoBackupEnabled, _lastAutoBackupTime, _availableAutoBackups) { ab, last, list -> Triple(ab, last, list) },
        _announcements
    ) { tb, settings, privacyPrefs, backupData, announcementsList ->
        SettingsUiState(
            transactionCount = tb.first.size,
            budgetCount = tb.second.size,
            isBiometricEnabled = settings.bio,
            isDailyReviewEnabled = settings.dr,
            dailyReviewHour = settings.h,
            dailyReviewMinute = settings.m,
            isShakeToHideEnabled = privacyPrefs.first,
            isDefaultPrivacyModeEnabled = privacyPrefs.second,
            isAutoBackupEnabled = backupData.first,
            lastAutoBackupTime = backupData.second,
            availableAutoBackups = backupData.third,
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
        _isShakeToHideEnabled.value = privacyModeManager.isShakeToHideEnabled()
        _isDefaultPrivacyModeEnabled.value = privacyModeManager.isDefaultPrivacyModeEnabled()
        _isAutoBackupEnabled.value = ScheduledBackupWorker.isAutoBackupEnabled(context)
        _lastAutoBackupTime.value = ScheduledBackupWorker.getLastBackupTime(context)
        _availableAutoBackups.value = ScheduledBackupWorker.getAvailableBackups(context)
        viewModelScope.launch {
            _announcements.value = promotionManager.getActiveAnnouncements()
        }
    }

    fun toggleShakeToHide(enabled: Boolean) {
        privacyModeManager.setShakeToHideEnabled(enabled)
        _isShakeToHideEnabled.value = enabled
    }

    fun toggleDefaultPrivacyMode(enabled: Boolean) {
        privacyModeManager.setDefaultPrivacyModeEnabled(enabled)
        _isDefaultPrivacyModeEnabled.value = enabled
    }

    fun toggleAutoBackup(context: Context, enabled: Boolean) {
        ScheduledBackupWorker.setAutoBackupEnabled(context, enabled)
        _isAutoBackupEnabled.value = enabled
    }

    fun performImmediateAutoBackup(context: Context, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = ScheduledBackupWorker.performBackup(context)
            if (success) {
                _lastAutoBackupTime.value = ScheduledBackupWorker.getLastBackupTime(context)
                _availableAutoBackups.value = ScheduledBackupWorker.getAvailableBackups(context)
            }
            onResult(success)
        }
    }

    fun loadAutoBackups(context: Context) {
        _availableAutoBackups.value = ScheduledBackupWorker.getAvailableBackups(context)
        _lastAutoBackupTime.value = ScheduledBackupWorker.getLastBackupTime(context)
    }

    fun restoreFromAutoBackupFile(
        file: File,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val jsonStr = file.readText()
            restoreBackupJson(jsonStr, context, onSuccess, onError)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to read backup file")
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
        val groups = splitDao.getAllGroupsList()
        val groupMembers = splitDao.getAllGroupMembersList()
        val splitExpenses = splitDao.getAllSplitExpensesList()
        val splitShares = splitDao.getAllExpenseSharesList()
        val groupSettlements = splitDao.getAllSettlementsList()
        return BackupManager.createBackupJson(
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            subscriptions = subscriptions,
            merchantMappings = merchantMappings,
            groups = groups,
            groupMembers = groupMembers,
            splitExpenses = splitExpenses,
            splitShares = splitShares,
            groupSettlements = groupSettlements
        )
    }

    fun restoreBackupJson(
        jsonStr: String,
        context: Context? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val data = BackupManager.parseBackupData(jsonStr)
                for (t in data.transactions) {
                    transactionRepository.insertTransaction(t)
                }
                for (b in data.budgets) {
                    budgetRepository.saveBudget(b)
                }
                for (c in data.categories) {
                    val existing = categoryDao.getCategoryByName(c.name)
                    if (existing != null) {
                        if (existing.emoji.isBlank() && c.emoji.isNotBlank()) {
                            categoryDao.updateCategoryInPlace(
                                id = existing.id,
                                newName = existing.name,
                                newType = existing.type,
                                newEmoji = c.emoji,
                                newColorHex = existing.colorHex
                            )
                        }
                    } else {
                        categoryDao.insertCategories(listOf(c))
                    }
                }
                for (s in data.subscriptions) {
                    val existing = subscriptionDao.findSubscriptionByName(s.name)
                    if (existing != null) {
                        subscriptionDao.updateSubscription(existing.copy(amount = s.amount, nextDueDateMs = s.nextDueDateMs, billingCycle = s.billingCycle))
                    } else {
                        subscriptionDao.insertSubscription(s)
                    }
                }
                for (m in data.merchantMappings) {
                    merchantCategoryMappingDao.insertMapping(m)
                }
                if (data.groups.isNotEmpty()) {
                    splitDao.insertGroups(data.groups)
                }
                if (data.groupMembers.isNotEmpty()) {
                    splitDao.insertMembers(data.groupMembers)
                }
                if (data.splitExpenses.isNotEmpty()) {
                    splitDao.insertExpenses(data.splitExpenses)
                }
                if (data.splitShares.isNotEmpty()) {
                    splitDao.insertShares(data.splitShares)
                }
                if (data.groupSettlements.isNotEmpty()) {
                    splitDao.insertSettlements(data.groupSettlements)
                }
                if (context != null) {
                    com.expensetracker.app.widget.ExpenseWidgetProvider.updateAllWidgets(context)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to restore backup")
            }
        }
    }
}

private data class Quad<A, B, C, D>(val bio: A, val dr: B, val h: C, val m: D)
