package com.expensetracker.app.ingestion.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.ingestion.parser.RegexTransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val packageName = sbn.packageName ?: return
            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val combined = "$title $text"

            if (combined.isBlank()) return

            val lowerText = combined.lowercase()
            // Check if notification contains transaction triggers
            if (lowerText.contains("rs") || lowerText.contains("inr") || lowerText.contains("₹") ||
                lowerText.contains("debited") || lowerText.contains("credited") || lowerText.contains("paid") || lowerText.contains("sent")
            ) {
                val parsed = RegexTransactionParser.parse(combined)
                if (parsed != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = ExpenseDatabase.getInstance(applicationContext, "expense_tracker_secret_passphrase_key".toByteArray())
                            val hash = "${parsed.merchant}-${parsed.amount}-${parsed.timestamp}"
                            val entity = TransactionEntity(
                                amount = parsed.amount,
                                type = parsed.type,
                                merchant = parsed.merchant,
                                rawText = combined,
                                category = parsed.category,
                                timestamp = parsed.timestamp,
                                transactionHash = hash
                            )
                            db.transactionDao().insertTransaction(entity)
                            Log.d("NotificationService", "Saved notification transaction: ${parsed.merchant} ₹${parsed.amount}")
                        } catch (e: Exception) {
                            Log.e("NotificationService", "Error saving notification transaction", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error processing notification", e)
        }
    }
}
