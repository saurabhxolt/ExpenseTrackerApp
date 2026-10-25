package com.expensetracker.app.ingestion.worker

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.security.SecurityKeyManager
import com.expensetracker.app.features.backup.BackupManager
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ScheduledBackupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean(KEY_ENABLED, true)
            if (!enabled) return Result.success()

            val success = performBackup(context)
            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "scheduled_local_backup_work"
        private const val PREFS_NAME = "backup_preferences"
        private const val KEY_ENABLED = "auto_backup_enabled"
        private const val KEY_LAST_BACKUP_TIME = "last_auto_backup_time"
        const val MAX_ROLLING_BACKUPS = 5

        fun isAutoBackupEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_ENABLED, true)
        }

        fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
            scheduleConfiguredBackup(context, forceReschedule = true)
        }

        fun getLastBackupTime(context: Context): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        }

        fun getBackupDirectory(context: Context): File {
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

        fun getAvailableBackups(context: Context): List<File> {
            val dir = getBackupDirectory(context)
            return dir.listFiles { file -> file.isFile && file.name.startsWith("backup_") && file.name.endsWith(".json") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }

        suspend fun performBackup(context: Context): Boolean {
            return try {
                val passphrase = SecurityKeyManager.getOrCreateDatabasePassphrase(context)
                val db = ExpenseDatabase.getInstance(context, passphrase)

                val transactions = db.transactionDao().getAllTransactionsList()
                val categories = db.categoryDao().getAllCategories().first()
                val budgets = db.budgetDao().getAllBudgets().first()
                val subscriptions = db.subscriptionDao().getAllSubscriptionsList()
                val merchantMappings = db.merchantCategoryMappingDao().getAllMappingsList()
                val groups = db.splitDao().getAllGroupsList()
                val groupMembers = db.splitDao().getAllGroupMembersList()
                val splitExpenses = db.splitDao().getAllSplitExpensesList()
                val splitShares = db.splitDao().getAllExpenseSharesList()
                val groupSettlements = db.splitDao().getAllSettlementsList()

                val json = BackupManager.createBackupJson(
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

                val dir = getBackupDirectory(context)
                val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFile = File(dir, "backup_auto_${timestampStr}.json")
                backupFile.writeText(json)

                // Enforce rolling retention: keep only MAX_ROLLING_BACKUPS
                val existingBackups = getAvailableBackups(context)
                if (existingBackups.size > MAX_ROLLING_BACKUPS) {
                    existingBackups.drop(MAX_ROLLING_BACKUPS).forEach { oldFile ->
                        oldFile.delete()
                    }
                }

                val now = System.currentTimeMillis()
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_BACKUP_TIME, now)
                    .apply()

                true
            } catch (e: Exception) {
                false
            }
        }

        fun scheduleConfiguredBackup(context: Context, forceReschedule: Boolean = false) {
            val workManager = WorkManager.getInstance(context)
            val enabled = isAutoBackupEnabled(context)

            if (!enabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            // Periodic backup every 7 days
            val request = PeriodicWorkRequestBuilder<ScheduledBackupWorker>(7, TimeUnit.DAYS)
                .build()

            val policy = if (forceReschedule) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                policy,
                request
            )
        }
    }
}
