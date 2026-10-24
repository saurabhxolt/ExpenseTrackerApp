package com.expensetracker.app.ingestion.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expensetracker.app.MainActivity
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyReviewWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        if (!enabled) {
            return Result.success()
        }

        val CHANNEL_ID = "daily_expense_review_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Expense Review Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily evening reminder to review & categorize today's expenses."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var todaySpend = 0.0
        try {
            val passphrase = com.expensetracker.app.core.security.SecurityKeyManager.getOrCreateDatabasePassphrase(context)
            val db = com.expensetracker.app.core.database.ExpenseDatabase.getInstance(context, passphrase)
            todaySpend = db.transactionDao().getTodaySpend(startOfDay) ?: 0.0
        } catch (e: Exception) {
            // Key derivation or database locked fallback
        }

        val contentText = if (todaySpend > 0.0) {
            "You recorded ₹${String.format(java.util.Locale.getDefault(), "%.2f", todaySpend)} in expenses today. Tap to review & organize them!"
        } else {
            "No expenses recorded today. Tap to add any cash transactions or check your balance!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🌙 End of Day Expense Review")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
        return Result.success()
    }

    companion object {
        const val PREFS_NAME = "daily_review_prefs"
        const val KEY_ENABLED = "daily_review_enabled"
        const val KEY_HOUR = "daily_review_hour"
        const val KEY_MINUTE = "daily_review_minute"
        const val WORK_NAME = "DailyExpenseReviewWork"

        fun isDailyReviewEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, true)
        }

        fun getDailyReviewTime(context: Context): Pair<Int, Int> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val hour = prefs.getInt(KEY_HOUR, 21) // 9:00 PM default
            val minute = prefs.getInt(KEY_MINUTE, 0)
            return Pair(hour, minute)
        }

        fun setDailyReviewPreferences(context: Context, enabled: Boolean, hour: Int, minute: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                putBoolean(KEY_ENABLED, enabled)
                putInt(KEY_HOUR, hour)
                putInt(KEY_MINUTE, minute)
                apply()
            }
            scheduleConfiguredReview(context, forceReschedule = true)
        }

        fun scheduleConfiguredReview(context: Context, forceReschedule: Boolean = false) {
            val workManager = WorkManager.getInstance(context)
            val enabled = isDailyReviewEnabled(context)

            if (!enabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val (hour, minute) = getDailyReviewTime(context)
            val initialDelayMs = calculateDelayUntilNextOccurrence(hour, minute)

            val request = PeriodicWorkRequestBuilder<DailyReviewWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
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

        private fun calculateDelayUntilNextOccurrence(targetHour: Int, targetMinute: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}
