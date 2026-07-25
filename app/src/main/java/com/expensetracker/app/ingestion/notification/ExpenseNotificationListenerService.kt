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

    private val bankingPackages = setOf(
        "com.google.android.apps.nfc.phone",
        "com.phonepe.app",
        "com.paytm",
        "com.dreamplug.androidapp", // CRED
        "com.hdfcbank.payzapp",
        "com.csam.icici.bank.imobile",
        "com.sbi.lotusintouch"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        if (bankingPackages.contains(packageName) || packageName.contains("bank", true)) {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val combined = "$title $text"

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
    }
}
