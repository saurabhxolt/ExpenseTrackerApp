package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.entity.SubscriptionEntity
import com.expensetracker.app.core.database.entity.TransactionEntity
import com.expensetracker.app.ingestion.parser.CategoryClassifier
import javax.inject.Inject
import javax.inject.Singleton

data class DetectedSubscriptionCandidate(
    val name: String,
    val amount: Double,
    val suggestedCycle: String = "Monthly",
    val category: String = "Subscriptions",
    val transactionCount: Int = 2
)

@Singleton
class SubscriptionDetector @Inject constructor() {

    fun detectCandidates(
        transactions: List<TransactionEntity>,
        existingSubscriptions: List<SubscriptionEntity>
    ): List<DetectedSubscriptionCandidate> {
        val existingNames = existingSubscriptions.map { it.name.lowercase().trim() }.toSet()

        val debitTrx = transactions.filter { it.type == "DEBIT" && it.amount > 0 }
        val groupedByMerchant = debitTrx.groupBy { CategoryClassifier.normalizeMerchantKey(it.merchant) }

        val candidates = mutableListOf<DetectedSubscriptionCandidate>()

        for ((key, list) in groupedByMerchant) {
            if (key.isBlank() || list.size < 2) continue
            val firstMerchant = list.first().merchant

            if (existingNames.any { it.contains(key) || key.contains(it) }) continue

            // Check if amounts are consistent (standard deviation or close values)
            val avgAmount = list.map { it.amount }.average()
            val isConsistentAmount = list.all { Math.abs(it.amount - avgAmount) / avgAmount <= 0.25 }

            if (isConsistentAmount) {
                val category = list.first().category.ifBlank { "Subscriptions" }
                candidates.add(
                    DetectedSubscriptionCandidate(
                        name = firstMerchant,
                        amount = avgAmount,
                        suggestedCycle = "Monthly",
                        category = category,
                        transactionCount = list.size
                    )
                )
            }
        }

        return candidates.sortedByDescending { it.transactionCount }.take(5)
    }
}
