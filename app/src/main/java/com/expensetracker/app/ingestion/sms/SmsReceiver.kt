package com.expensetracker.app.ingestion.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.expensetracker.app.ingestion.parser.RegexTransactionParser

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.messageBody ?: continue
                val parsed = RegexTransactionParser.parse(body)
                if (parsed != null) {
                    Log.d("SmsReceiver", "Parsed transaction: ${parsed.amount} at ${parsed.merchant}")
                    // Save to Room DB asynchronously via WorkManager / Application scope
                }
            }
        }
    }
}
