package com.expensetracker.app.core.analysis

import com.expensetracker.app.core.database.entity.TransactionEntity
import kotlin.math.roundToInt

data class PriceHikeAlert(
    val merchant: String,
    val previousAmount: Double,
    val currentAmount: Double,
    val difference: Double,
    val percentageIncrease: Double,
    val previousDateMs: Long,
    val currentDateMs: Long
)

object PriceCreepDetector {

    /**
     * Minimum percentage increase to trigger an alert (3.0%).
     * Filters out sub-1% or tiny penny fluctuations due to rounding or minor taxes.
     */
    private const val MIN_PERCENT_INCREASE = 3.0

    /**
     * Minimum absolute difference in ₹ (e.g. ₹5.00).
     */
    private const val MIN_ABSOLUTE_DIFFERENCE = 5.0

    /**
     * Checks if a particular transaction represents a price creep compared to prior debits for the same merchant.
     */
    fun detectPriceHikeForTransaction(
        transaction: TransactionEntity,
        allTransactions: List<TransactionEntity>
    ): PriceHikeAlert? {
        if (transaction.type != "DEBIT" || transaction.merchant.isBlank() || transaction.merchant.equals("Unknown", ignoreCase = true)) {
            return null
        }

        val normTarget = normalizeMerchant(transaction.merchant)
        if (normTarget.length < 3) return null

        val priorDebits = allTransactions.filter { t ->
            t.id != transaction.id &&
            t.type == "DEBIT" &&
            t.timestamp < transaction.timestamp &&
            normalizeMerchant(t.merchant) == normTarget
        }.sortedByDescending { it.timestamp }

        if (priorDebits.isEmpty()) return null

        val prior = priorDebits.first()
        val diff = transaction.amount - prior.amount
        if (diff >= MIN_ABSOLUTE_DIFFERENCE) {
            val pct = (diff / prior.amount) * 100.0
            if (pct >= MIN_PERCENT_INCREASE) {
                return PriceHikeAlert(
                    merchant = transaction.merchant,
                    previousAmount = prior.amount,
                    currentAmount = transaction.amount,
                    difference = diff,
                    percentageIncrease = (pct * 10.0).roundToInt() / 10.0,
                    previousDateMs = prior.timestamp,
                    currentDateMs = transaction.timestamp
                )
            }
        }
        return null
    }

    /**
     * Detects price hike for a subscription given its name and historical transactions.
     */
    fun detectPriceHikeForSubscription(
        subscriptionName: String,
        currentAmount: Double,
        allTransactions: List<TransactionEntity>
    ): PriceHikeAlert? {
        val normTarget = normalizeMerchant(subscriptionName)
        if (normTarget.length < 3) return null

        val matchingDebits = allTransactions.filter { t ->
            t.type == "DEBIT" && (
                normalizeMerchant(t.merchant).contains(normTarget) ||
                normTarget.contains(normalizeMerchant(t.merchant))
            )
        }.sortedByDescending { it.timestamp }

        if (matchingDebits.isEmpty()) return null

        val latest = matchingDebits.first()
        val olderDebits = matchingDebits.filter { it.timestamp < latest.timestamp }

        if (olderDebits.isNotEmpty()) {
            val prior = olderDebits.first()
            val diff = latest.amount - prior.amount
            if (diff >= MIN_ABSOLUTE_DIFFERENCE) {
                val pct = (diff / prior.amount) * 100.0
                if (pct >= MIN_PERCENT_INCREASE) {
                    return PriceHikeAlert(
                        merchant = subscriptionName,
                        previousAmount = prior.amount,
                        currentAmount = latest.amount,
                        difference = diff,
                        percentageIncrease = (pct * 10.0).roundToInt() / 10.0,
                        previousDateMs = prior.timestamp,
                        currentDateMs = latest.timestamp
                    )
                }
            }
        } else if (currentAmount > 0.0) {
            // Compare current subscription amount to earlier debits if different
            val diff = currentAmount - latest.amount
            if (diff >= MIN_ABSOLUTE_DIFFERENCE) {
                val pct = (diff / latest.amount) * 100.0
                if (pct >= MIN_PERCENT_INCREASE) {
                    return PriceHikeAlert(
                        merchant = subscriptionName,
                        previousAmount = latest.amount,
                        currentAmount = currentAmount,
                        difference = diff,
                        percentageIncrease = (pct * 10.0).roundToInt() / 10.0,
                        previousDateMs = latest.timestamp,
                        currentDateMs = System.currentTimeMillis()
                    )
                }
            }
        }

        return null
    }

    /**
     * Normalizes merchant/brand names by removing generic legal and rail suffixes.
     */
    fun normalizeMerchant(name: String): String {
        return name.lowercase()
            .replace(Regex("\\b(pvt|ltd|limited|services|app|pay|india|in|corp|inc|technologies|solutions)\\b"), "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
    }
}
