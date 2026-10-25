package com.expensetracker.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.expensetracker.app.MainActivity
import com.expensetracker.app.core.analysis.SalaryProvisioningCalculator
import com.expensetracker.app.core.database.ExpenseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SalaryProvisioningNotificationHelper {

    private const val CHANNEL_ID = "salary_provisioning_channel"
    private const val NOTIFICATION_ID = 2026

    fun triggerSalaryDayNotification(context: Context, salaryAmount: Double, db: ExpenseDatabase) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Salary Day & Policy Stash Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to stash funds for upcoming annual insurance policies on salary day."
            }
            notificationManager.createNotificationChannel(channel)
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val policies = db.insurancePolicyDao().getAllPoliciesList()
                val jars = db.savingsGoalDao().getAllGoalsList()

                val plan = SalaryProvisioningCalculator.calculatePlan(policies, jars)
                if (plan.totalRecommendedStash <= 0 || plan.activePolicyCount == 0) {
                    return@launch
                }

                val formattedSalary = "₹" + String.format(java.util.Locale.getDefault(), "%,.0f", salaryAmount)
                val formattedStash = "₹" + String.format(java.util.Locale.getDefault(), "%,.0f", plan.totalRecommendedStash)

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("NAVIGATE_TO", "SALARY_STASH")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                    .setContentTitle("💰 Salary Credited ($formattedSalary)!")
                    .setContentText("Stash $formattedStash today across ${plan.activePolicyCount} annual policies to stay prepared.")
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(
                            "Salary of $formattedSalary has been credited. You have ${plan.activePolicyCount} upcoming insurance policies (Health, Term, Car/Bike). Recommended stash today: $formattedStash into your Sinking Jars to prevent annual lump-sum shock."
                        )
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (_: Exception) {
            }
        }
    }
}
