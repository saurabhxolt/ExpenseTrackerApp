package com.expensetracker.app.ingestion.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.ingestion.parser.RegexTransactionParser
import java.util.Calendar
import java.util.TimeZone

object SmsScanner {

    private const val PREFS_NAME = "expense_tracker_prefs"
    private const val KEY_LAST_SCAN_TIME = "last_sms_scan_timestamp"

    fun scanInbox(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastScanTime = prefs.getLong(KEY_LAST_SCAN_TIME, 0L)

        // If never scanned before, calculate 1st day of current month (00:00:00)
        val startTime = if (lastScanTime == 0L) {
            getStartOfCurrentMonthTimestamp()
        } else {
            lastScanTime
        }

        Log.d("SmsScanner", "Scanning SMS inbox from timestamp: $startTime")
        var newTransactionsCount = 0

        try {
            val contentResolver = context.contentResolver
            val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ADDRESS)
            val selection = "${Telephony.Sms.DATE} >= ?"
            val selectionArgs = arrayOf(startTime.toString())
            val sortOrder = "${Telephony.Sms.DATE} ASC"

            val cursor = contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)

                val db = ExpenseDatabase.getInstance(context.applicationContext, "expense_tracker_secret_passphrase_key".toByteArray())

                while (c.moveToNext()) {
                    val body = c.getString(bodyIndex) ?: continue
                    val smsDate = c.getLong(dateIndex)

                    val parsed = RegexTransactionParser.parse(body)
                    if (parsed != null) {
                        val hash = "${parsed.merchant}-${parsed.amount}-$smsDate"

                        // Insert transaction synchronously into DB
                        val entity = TransactionEntity(
                            amount = parsed.amount,
                            type = parsed.type,
                            merchant = parsed.merchant,
                            rawText = body,
                            category = parsed.category,
                            timestamp = smsDate,
                            transactionHash = hash
                        )

                        // Run blocking insertion on background thread
                        val rowId = kotlinx.coroutines.runBlocking {
                            db.transactionDao().insertTransaction(entity)
                        }
                        if (rowId > 0) {
                            newTransactionsCount++
                        }
                    }
                }
            }

            // Save new last scan timestamp
            prefs.edit().putLong(KEY_LAST_SCAN_TIME, System.currentTimeMillis()).apply()
            Log.d("SmsScanner", "SMS Scan complete! Found $newTransactionsCount new transactions.")

        } catch (e: Exception) {
            Log.e("SmsScanner", "Error scanning SMS inbox", e)
        }

        return newTransactionsCount
    }

    private fun getStartOfCurrentMonthTimestamp(): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
