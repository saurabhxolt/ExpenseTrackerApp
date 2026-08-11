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

    fun normalizeBrandName(name: String): String {
        return name.lowercase()
            .replace(Regex("(?i)\\b(au|pvt|ltd|limited|upi|autopay|mandate|app|pay|india|broking|services|inc|club|corp|official|trf|ref|no|refno)\\b"), "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }

    suspend fun autoDetectAndSave(
        repository: SubscriptionRepository,
        parsedTransaction: ParsedTransaction
    ) {
        if (!isRecurringMandateText(parsedTransaction.rawText)) return

        val allSubs = repository.getAllSubscriptionsList()
        val targetNorm = normalizeBrandName(parsedTransaction.merchant)

        val existing = allSubs.firstOrNull { sub ->
            val subNorm = normalizeBrandName(sub.name)
            subNorm == targetNorm || (subNorm.length >= 4 && targetNorm.contains(subNorm)) || (targetNorm.length >= 4 && subNorm.contains(targetNorm))
        }

        val cycle = detectBillingCycle(parsedTransaction.rawText)
        val nextDueMs = computeNextDueDate(parsedTransaction.rawText)

        if (existing == null) {
            val entity = SubscriptionEntity(
                name = parsedTransaction.merchant,
                amount = parsedTransaction.amount,
                billingCycle = cycle,
                nextDueDateMs = nextDueMs,
                category = if (parsedTransaction.category.isNotBlank() && parsedTransaction.category != "General Expense") parsedTransaction.category else "Subscriptions",
                isActive = true,
                isAutoDetected = true,
                rawText = parsedTransaction.rawText
            )
            repository.saveSubscription(entity)
        } else {
            // Late SMS / Multi-Notification Deduplication Check:
            // If the existing subscription due date is already set in the future (further than 15 days out),
            // a late/duplicate SMS for the same cycle should update amount and raw text without advancing due date twice.
            val nowMs = System.currentTimeMillis()
            val finalNextDueMs = if (existing.nextDueDateMs > nowMs + (15L * 24 * 60 * 60 * 1000L)) {
                existing.nextDueDateMs
            } else {
                nextDueMs
            }

            val updated = existing.copy(
                amount = parsedTransaction.amount,
                billingCycle = if (existing.billingCycle.isBlank()) cycle else existing.billingCycle,
                nextDueDateMs = finalNextDueMs,
                rawText = parsedTransaction.rawText
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
