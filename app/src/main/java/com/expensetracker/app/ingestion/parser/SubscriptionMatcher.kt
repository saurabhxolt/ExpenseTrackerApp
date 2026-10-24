package com.expensetracker.app.ingestion.parser

import com.expensetracker.app.core.database.dao.SubscriptionDao
import com.expensetracker.app.core.database.entity.SubscriptionEntity
import com.expensetracker.app.core.database.entity.TransactionEntity
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionMatcher @Inject constructor(
    private val subscriptionDao: SubscriptionDao
) {

    suspend fun matchAndAdvance(transaction: TransactionEntity): SubscriptionEntity? {
        if (transaction.type != "DEBIT") return null

        val activeSubscriptions = subscriptionDao.getAllSubscriptionsList().filter { it.isActive }
        if (activeSubscriptions.isEmpty()) return null

        val lowerMerchant = transaction.merchant.lowercase().trim()
        val lowerRawText = transaction.rawText.lowercase().trim()

        val matched = activeSubscriptions.firstOrNull { sub ->
            val lowerSubName = sub.name.lowercase().trim()
            if (lowerSubName.isBlank()) return@firstOrNull false

            val nameMatched = lowerMerchant.contains(lowerSubName) ||
                    lowerRawText.contains(lowerSubName) ||
                    lowerSubName.contains(lowerMerchant.takeIf { it.length >= 3 } ?: "___")

            val amountDiffRatio = if (sub.amount > 0) Math.abs(transaction.amount - sub.amount) / sub.amount else 0.0
            val amountMatched = amountDiffRatio <= 0.20 // Within 20% price variance or exact match

            nameMatched && amountMatched
        }

        if (matched != null) {
            val updatedDueDate = calculateNextDueDate(matched.nextDueDateMs, matched.billingCycle)
            val updated = matched.copy(
                nextDueDateMs = updatedDueDate,
                lastPaidTransactionId = transaction.id,
                autoMatched = true,
                lastPaidDateMs = transaction.timestamp
            )
            subscriptionDao.updateSubscription(updated)
            return updated
        }

        return null
    }

    companion object {
        fun calculateNextDueDate(currentDueDateMs: Long, billingCycle: String): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = Math.max(currentDueDateMs, System.currentTimeMillis())

            when (billingCycle.lowercase()) {
                "weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "quarterly" -> cal.add(Calendar.MONTH, 3)
                "yearly" -> cal.add(Calendar.YEAR, 1)
                else -> cal.add(Calendar.MONTH, 1) // Default Monthly
            }
            return cal.timeInMillis
        }
    }
}
