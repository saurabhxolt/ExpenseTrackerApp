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

import com.expensetracker.app.ingestion.deduplication.TransactionDeduplicator

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = ExpenseDatabase.getInstance(context.applicationContext, "expense_tracker_secret_passphrase_key".toByteArray())
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
                            val rowId = TransactionDeduplicator.insertWithDeduplication(db.transactionDao(), entity)
                            if (rowId > 0) {
                                Log.d("SmsReceiver", "Saved SMS transaction: ${parsed.merchant} ₹${parsed.amount}")
                            } else {
                                Log.d("SmsReceiver", "Duplicate SMS transaction suppressed: ${parsed.merchant} ₹${parsed.amount}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error saving SMS transaction", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
