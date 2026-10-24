package com.expensetracker.app.ingestion.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.expensetracker.app.core.database.ExpenseDatabase
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.ingestion.deduplication.TransactionDeduplicator
import com.expensetracker.app.ingestion.parser.RegexTransactionParser
import java.util.Calendar
import java.util.TimeZone

object SmsScanner {

    private const val PREFS_NAME = "expense_tracker_prefs"
    private const val KEY_LAST_SCAN_TIME = "last_sms_scan_timestamp"

    fun scanInbox(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastScanTime = prefs.getLong(KEY_LAST_SCAN_TIME, 0L)

        // If never scanned before, calculate 1st day of 3 months prior (00:00:00)
        // e.g., if current month is October, scan starts from 1st July 00:00:00
        val startTime = if (lastScanTime == 0L) {
            getInitialScanTimestamp(monthsBack = 3)
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

            val passphrase = com.expensetracker.app.core.security.SecurityKeyManager.getOrCreateDatabasePassphrase(context.applicationContext)
            val db = ExpenseDatabase.getInstance(context.applicationContext, passphrase)
            val mappingDao = db.merchantCategoryMappingDao()

            val pendingEntities = mutableListOf<TransactionEntity>()

            cursor?.use { c ->
                val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)

                while (c.moveToNext()) {
                    val body = c.getString(bodyIndex) ?: continue
                    val smsDate = c.getLong(dateIndex)

                    val parsed = RegexTransactionParser.parse(body)
                    if (parsed != null) {
                        val hash = "${parsed.merchant}-${parsed.amount}-$smsDate"

                        pendingEntities.add(
                            TransactionEntity(
                                amount = parsed.amount,
                                type = parsed.type,
                                merchant = parsed.merchant,
                                rawText = body,
                                category = parsed.category,
                                timestamp = smsDate,
                                transactionHash = hash
                            )
                        )
                    }
                }
            }

            val cleanedCount = kotlinx.coroutines.runBlocking {
                val subRepo = com.expensetracker.app.core.data.repository.SubscriptionRepository(db.subscriptionDao())
                for (entity in pendingEntities) {
                    val normKey = com.expensetracker.app.ingestion.parser.CategoryClassifier.normalizeMerchantKey(entity.merchant)
                    val learnedCategory = if (normKey.isNotBlank()) {
                        mappingDao.getMappingForMerchant(normKey)?.assignedCategory
                    } else null
                    val finalEntity = if (!learnedCategory.isNullOrBlank()) {
                        entity.copy(category = learnedCategory)
                    } else {
                        entity
                    }

                    val rowId = TransactionDeduplicator.insertWithDeduplication(db.transactionDao(), finalEntity)
                    if (rowId > 0) {
                        newTransactionsCount++
                        val parsedTrx = com.expensetracker.app.ingestion.parser.ParsedTransaction(
                            amount = finalEntity.amount,
                            type = finalEntity.type,
                            merchant = finalEntity.merchant,
                            rawText = finalEntity.rawText,
                            category = finalEntity.category,
                            timestamp = finalEntity.timestamp,
                            accountDigits = finalEntity.accountDigits
                        )
                        com.expensetracker.app.ingestion.parser.RecurringMandateDetector.autoDetectAndSave(subRepo, parsedTrx)
                    }
                }
                TransactionDeduplicator.cleanupExistingDuplicates(db.transactionDao())
            }

            // Save new last scan timestamp
            prefs.edit().putLong(KEY_LAST_SCAN_TIME, System.currentTimeMillis()).apply()
            Log.d("SmsScanner", "SMS Scan complete! Found $newTransactionsCount new transactions, cleaned $cleanedCount existing duplicates.")

            if (newTransactionsCount > 0) {
                com.expensetracker.app.widget.ExpenseWidgetProvider.updateAllWidgets(context)
            }

        } catch (e: Exception) {
            Log.e("SmsScanner", "Error scanning SMS inbox", e)
        }

        return newTransactionsCount
    }

    fun getInitialScanTimestamp(
        monthsBack: Int = 3,
        baseCalendar: Calendar = Calendar.getInstance(TimeZone.getDefault())
    ): Long {
        val calendar = baseCalendar.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MONTH, -monthsBack)
        return calendar.timeInMillis
    }
}
