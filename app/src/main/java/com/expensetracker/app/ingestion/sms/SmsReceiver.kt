package com.expensetracker.app.ingestion.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.ingestion.parser.RegexTransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = ExpenseDatabase.getInstance(context, "expense_tracker_secret_passphrase_key".toByteArray())
                    for (sms in messages) {
                        val body = sms.messageBody ?: continue
                        val parsed = RegexTransactionParser.parse(body)
                        if (parsed != null) {
                            val hash = "${parsed.merchant}-${parsed.amount}-${parsed.timestamp}"
                            val entity = TransactionEntity(
                                amount = parsed.amount,
                                type = parsed.type,
                                merchant = parsed.merchant,
                                rawText = body,
                                category = parsed.category,
                                timestamp = parsed.timestamp,
                                transactionHash = hash
                            )
                            db.transactionDao().insertTransaction(entity)
                            Log.d("SmsReceiver", "Saved transaction to DB: ${parsed.merchant} ₹${parsed.amount}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error saving transaction", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
