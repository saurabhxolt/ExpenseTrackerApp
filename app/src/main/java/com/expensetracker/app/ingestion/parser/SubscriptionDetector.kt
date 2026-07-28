package com.expensetracker.app.ingestion.parser

import com.expensetracker.app.core.data.repository.SubscriptionRepository
import com.expensetracker.app.core.database.entity.SubscriptionEntity
import java.util.Calendar

object SubscriptionDetector {

    private val RECURRING_KEYWORDS = listOf(
        "autopay", "e-mandate", "mandate", "recurring", "standing instruction",
        "subscription", "auto-debit", "auto debit", "plan renewed", "renewal",
        "netflix", "spotify", "youtube", "prime video", "hotstar", "apple.com/bill",
        "google play", "jio recharge", "airtel bill", "rent payment", "sip"
    )

    fun isRecurringMandateText(rawText: String): Boolean {
        val lower = rawText.lowercase()
        return RECURRING_KEYWORDS.any { lower.contains(it) }
    }

    suspend fun autoDetectAndSave(
        repository: SubscriptionRepository,
        parsedTransaction: ParsedTransaction
    ) {
        if (!isRecurringMandateText(parsedTransaction.rawText)) return

        val existing = repository.findSubscriptionByName(parsedTransaction.merchant)
        val nextDueMs = computeNextDueDate(parsedTransaction.rawText)

        if (existing == null) {
            val entity = SubscriptionEntity(
                name = parsedTransaction.merchant,
                amount = parsedTransaction.amount,
                billingCycle = detectBillingCycle(parsedTransaction.rawText),
                nextDueDateMs = nextDueMs,
                category = if (parsedTransaction.category.isNotBlank()) parsedTransaction.category else "Subscriptions",
                isActive = true,
                isAutoDetected = true,
                rawText = parsedTransaction.rawText
            )
            repository.saveSubscription(entity)
        } else {
            val updated = existing.copy(
                amount = parsedTransaction.amount,
                nextDueDateMs = nextDueMs
            )
            repository.updateSubscription(updated)
        }
    }

    private fun detectBillingCycle(rawText: String): String {
        val lower = rawText.lowercase()
        return when {
            lower.contains("annual") || lower.contains("yearly") -> "Yearly"
            lower.contains("weekly") -> "Weekly"
            lower.contains("quarterly") -> "Quarterly"
            else -> "Monthly"
        }
    }

    private fun computeNextDueDate(rawText: String): Long {
        val c = Calendar.getInstance()
        val lower = rawText.lowercase()
        if (lower.contains("annual") || lower.contains("yearly")) {
            c.add(Calendar.YEAR, 1)
        } else if (lower.contains("weekly")) {
            c.add(Calendar.DAY_OF_YEAR, 7)
        } else if (lower.contains("quarterly")) {
            c.add(Calendar.MONTH, 3)
        } else {
            c.add(Calendar.MONTH, 1)
        }
        return c.timeInMillis
    }
}
