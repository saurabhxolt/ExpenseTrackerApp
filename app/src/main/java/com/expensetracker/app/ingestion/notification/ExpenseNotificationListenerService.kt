package com.expensetracker.app.ingestion.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.expensetracker.app.ingestion.parser.RegexTransactionParser

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
                Log.d("NotificationService", "Parsed transaction: ${parsed.amount} at ${parsed.merchant}")
                // Save to Room DB asynchronously via WorkManager / Application scope
            }
        }
    }
}
