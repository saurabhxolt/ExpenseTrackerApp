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

import com.expensetracker.app.ingestion.deduplication.TransactionDeduplicator

class ExpenseNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            if (sbn.packageName.isNullOrBlank()) return
            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val combined = "$title $text"

            if (combined.isBlank()) return

            val lowerText = combined.lowercase()
            // Check if notification contains transaction triggers
            if (lowerText.contains("rs") || lowerText.contains("inr") || lowerText.contains("₹") ||
                lowerText.contains("debited") || lowerText.contains("credited") || lowerText.contains("paid") || lowerText.contains("sent") ||
                lowerText.contains("spent") || lowerText.contains("received") || lowerText.contains("withdrawn") || lowerText.contains("deducted")
            ) {
                val parsed = RegexTransactionParser.parse(combined)
                if (parsed != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val passphrase = com.expensetracker.app.core.security.SecurityKeyManager.getOrCreateDatabasePassphrase(applicationContext)
                            val db = ExpenseDatabase.getInstance(applicationContext, passphrase)
                            val mappingDao = db.merchantCategoryMappingDao()

                            val notifTimestamp = sbn.postTime.takeIf { it > 0 } ?: parsed.timestamp

                            // Check learned merchant category preference
                            val normKey = com.expensetracker.app.ingestion.parser.CategoryClassifier.normalizeMerchantKey(parsed.merchant)
                            val learnedCategory = if (normKey.isNotBlank()) {
                                mappingDao.getMappingForMerchant(normKey)?.assignedCategory
                            } else null
                            val finalCategory = learnedCategory?.takeIf { it.isNotBlank() } ?: parsed.category

                            val hash = "${parsed.merchant}-${parsed.amount}-$notifTimestamp"
                            val entity = TransactionEntity(
                                amount = parsed.amount,
                                type = parsed.type,
                                merchant = parsed.merchant,
                                rawText = combined,
                                category = finalCategory,
                                timestamp = notifTimestamp,
                                transactionHash = hash
                            )
                            val rowId = TransactionDeduplicator.insertWithDeduplication(db.transactionDao(), entity)
                            if (rowId > 0) {
                                Log.d("NotificationService", "Saved notification transaction: ${parsed.merchant} ₹${parsed.amount}")
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
                                                    applicationContext,
                                                    entity.amount,
                                                    db
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("NotificationService", "Error reconciling recurring income", e)
                                    }
                                }

                                com.expensetracker.app.widget.ExpenseWidgetProvider.updateAllWidgets(applicationContext)
                            } else {
                                Log.d("NotificationService", "Duplicate notification transaction suppressed: ${parsed.merchant} ₹${parsed.amount}")
                            }
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
