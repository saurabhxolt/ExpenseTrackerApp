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
                    val passphrase = com.expensetracker.app.core.security.SecurityKeyManager.getOrCreateDatabasePassphrase(context.applicationContext)
                    val db = ExpenseDatabase.getInstance(context.applicationContext, passphrase)
                    val mappingDao = db.merchantCategoryMappingDao()

                    for (sms in messages) {
                        val body = sms.messageBody ?: continue
                        val parsed = RegexTransactionParser.parse(body)
                        if (parsed != null) {
                            val smsTimestamp = sms.timestampMillis.takeIf { it > 0 } ?: parsed.timestamp

                            // Check learned merchant category preference
                            val normKey = com.expensetracker.app.ingestion.parser.CategoryClassifier.normalizeMerchantKey(parsed.merchant)
                            val learnedCategory = if (normKey.isNotBlank()) {
                                mappingDao.getMappingForMerchant(normKey)?.assignedCategory
                            } else null
                            val finalCategory = learnedCategory?.takeIf { it.isNotBlank() } ?: parsed.category

                            val hash = "${parsed.merchant}-${parsed.amount}-$smsTimestamp"
                            val entity = TransactionEntity(
                                amount = parsed.amount,
                                type = parsed.type,
                                merchant = parsed.merchant,
                                rawText = body,
                                category = finalCategory,
                                timestamp = smsTimestamp,
                                transactionHash = hash
                            )
                            val rowId = TransactionDeduplicator.insertWithDeduplication(db.transactionDao(), entity)
                            if (rowId > 0) {
                                Log.d("SmsReceiver", "Saved SMS transaction: ${parsed.merchant} ₹${parsed.amount}")
                                val subRepo = com.expensetracker.app.core.data.repository.SubscriptionRepository(db.subscriptionDao())
                                com.expensetracker.app.ingestion.parser.RecurringMandateDetector.autoDetectAndSave(subRepo, parsed)

                                if (entity.type.equals("CREDIT", ignoreCase = true)) {
                                    try {
                                        val incomeDao = db.recurringIncomeDao()
                                        val incomes = incomeDao.getAllIncomesList()
                                        val match = com.expensetracker.app.core.analysis.RecurringInflowReconciler.reconcile(entity, incomes)
                                        if (match != null) {
                                            val nextDue = com.expensetracker.app.core.data.repository.RecurringIncomeRepository.calculateNextDueDate(
                                                match.recurringIncome.nextDueDateMs,
                                                match.recurringIncome.frequency
                                            )
                                            incomeDao.updateIncome(
                                                match.recurringIncome.copy(
                                                    lastReceivedDateMs = System.currentTimeMillis(),
                                                    matchedTransactionId = rowId,
                                                    nextDueDateMs = nextDue
                                                )
                                            )
                                            if (match.isSalary) {
                                                com.expensetracker.app.core.notifications.SalaryProvisioningNotificationHelper.triggerSalaryDayNotification(
                                                    context,
                                                    entity.amount,
                                                    db
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SmsReceiver", "Error reconciling recurring income", e)
                                    }
                                }

                                com.expensetracker.app.widget.ExpenseWidgetProvider.updateAllWidgets(context)
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
