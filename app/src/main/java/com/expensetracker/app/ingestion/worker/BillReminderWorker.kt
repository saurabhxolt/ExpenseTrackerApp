package com.expensetracker.app.ingestion.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.expensetracker.app.MainActivity
import com.expensetracker.app.core.database.dao.TransactionDao
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BillReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val CHANNEL_ID = "bill_reminders_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bill & Subscription Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you 2 days before upcoming subscriptions & EMI bills are due."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val passphrase = try {
            com.expensetracker.app.core.security.SecurityKeyManager.getOrCreateDatabasePassphrase(context)
        } catch (e: Exception) {
            return Result.success()
        }

        val db = com.expensetracker.app.core.database.ExpenseDatabase.getInstance(context, passphrase)
        val allSubs = db.subscriptionDao().getAllSubscriptionsList()
        val now = System.currentTimeMillis()
        val dueWindowEnd = now + 48 * 60 * 60 * 1000L // Next 48 hours

        val dueBills = allSubs.filter { it.isActive && it.nextDueDateMs in now..dueWindowEnd }
        if (dueBills.isEmpty()) {
            return Result.success() // No bills due in next 48 hours -> suppress false notification
        }

        val title = if (dueBills.size == 1) "Upcoming Bill Reminder 🔔" else "${dueBills.size} Upcoming Bills Due 🔔"
        val message = if (dueBills.size == 1) {
            val bill = dueBills.first()
            "${bill.name} (₹${String.format(java.util.Locale.getDefault(), "%.2f", bill.amount)}) is due soon. Tap to view details."
        } else {
            val totalDue = dueBills.sumOf { it.amount }
            "${dueBills.size} recurring bills totaling ₹${String.format(java.util.Locale.getDefault(), "%.2f", totalDue)} are due in the next 48 hours."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
        return Result.success()
    }

    companion object {
        fun schedulePeriodicReminders(context: Context) {
            val request = PeriodicWorkRequestBuilder<BillReminderWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "BillReminderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
