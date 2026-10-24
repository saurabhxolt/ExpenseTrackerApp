package com.expensetracker.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.expensetracker.app.MainActivity
import com.expensetracker.app.R
import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.security.SecurityKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class ExpenseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val key = SecurityKeyManager.getOrCreateDatabasePassphrase(context)
                val db = ExpenseDatabase.getInstance(context, key)
                val monthCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val netBalance = db.transactionDao().getNetBalanceSince(monthCal.timeInMillis)

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todaySpend = db.transactionDao().getTodaySpend(cal.timeInMillis) ?: 0.0

                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        val intent = Intent(context, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            0,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )

                        val views = RemoteViews(context.packageName, R.layout.widget_expense).apply {
                            setOnClickPendingIntent(R.id.widget_title, pendingIntent)
                            setOnClickPendingIntent(R.id.widget_net_balance, pendingIntent)
                            setOnClickPendingIntent(R.id.widget_today_spend, pendingIntent)
                            setTextViewText(
                                R.id.widget_net_balance,
                                String.format(Locale.getDefault(), "₹%,.2f", netBalance)
                            )
                            setTextViewText(
                                R.id.widget_today_spend,
                                String.format(Locale.getDefault(), "Today's Spend: ₹%,.2f", todaySpend)
                            )
                        }

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } catch (e: Exception) {
                // Ignore key derivation / database locked exceptions when device is in locked state
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, ExpenseWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val intent = Intent(context, ExpenseWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (ignored: Exception) {
            }
        }
    }
}
