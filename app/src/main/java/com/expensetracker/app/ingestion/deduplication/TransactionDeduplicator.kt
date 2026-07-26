package com.expensetracker.app.ingestion.deduplication

import com.expensetracker.app.core.database.dao.TransactionDao
import com.expensetracker.app.core.database.entity.TransactionEntity

object TransactionDeduplicator {

    private const val DEDUPLICATION_WINDOW_MS = 10 * 60 * 1000L

    suspend fun insertWithDeduplication(dao: TransactionDao, entity: TransactionEntity): Long {
        // 1. Timeless Credit Card Payment Deduplication
        val isCreditCardPayment = entity.merchant.contains("Credit Card", ignoreCase = true) ||
                                 entity.rawText.contains("credit card", ignoreCase = true) ||
                                 entity.rawText.contains("bobcard", ignoreCase = true)

        if (isCreditCardPayment) {
            val existingCardPayment = dao.findExistingCreditCardPayment(entity.amount)
            if (existingCardPayment != null) {
                if (isGenericMerchant(existingCardPayment.merchant) && !isGenericMerchant(entity.merchant)) {
                    val updated = existingCardPayment.copy(
                        merchant = entity.merchant,
                        category = entity.category
                    )
                    dao.updateTransaction(updated)
                }
                return -1L // Suppress duplicate credit card payment confirmation regardless of time delay
            }
        }

        // 2. Candidate Duplicate Search within ±10 minutes window
        val minTime = entity.timestamp - DEDUPLICATION_WINDOW_MS
        val maxTime = entity.timestamp + DEDUPLICATION_WINDOW_MS

        val existing = dao.findCandidateDuplicate(
            type = entity.type,
            amount = entity.amount,
            minTimestamp = minTime,
            maxTimestamp = maxTime
        )

        if (existing != null) {
            val sameRawText = existing.rawText.trim().equals(entity.rawText.trim(), ignoreCase = true)
            val merchantsMatchOrRelated = areMerchantsMatchingOrRelated(existing.merchant, entity.merchant)
            val bothAreRefunds = existing.rawText.lowercase().contains("refund") && entity.rawText.lowercase().contains("refund")
            val bothAreCashback = existing.rawText.lowercase().contains("cashback") && entity.rawText.lowercase().contains("cashback")
            val isExistingGeneric = isGenericMerchant(existing.merchant)
            val isNewGeneric = isGenericMerchant(entity.merchant)

            // Suppress duplicate if raw text matches, merchants match/related, both are refunds, both are cashbacks, or one is generic
            if (sameRawText || merchantsMatchOrRelated || bothAreRefunds || bothAreCashback || isExistingGeneric || isNewGeneric) {
                // Enrich existing record with richer merchant name if applicable
                if (isExistingGeneric && !isNewGeneric) {
                    val updated = existing.copy(
                        merchant = entity.merchant,
                        category = entity.category
                    )
                    dao.updateTransaction(updated)
                } else if (entity.merchant.length > existing.merchant.length && isBrandMatch(existing.merchant, entity.merchant)) {
                    val updated = existing.copy(
                        merchant = entity.merchant,
                        category = entity.category
                    )
                    dao.updateTransaction(updated)
                }
                return -1L // Duplicate suppressed
            }
            // If merchants are distinct (e.g. Swiggy vs Zomato), proceed with insertion below
        }

        return dao.insertTransaction(entity)
    }

    suspend fun cleanupExistingDuplicates(dao: TransactionDao): Int {
        val allTxns = dao.getAllTransactionsList()
        val toDelete = mutableSetOf<TransactionEntity>()

        for (i in 0 until allTxns.size) {
            val current = allTxns[i]
            if (toDelete.contains(current)) continue

            for (j in i + 1 until allTxns.size) {
                val candidate = allTxns[j]
                if (toDelete.contains(candidate)) continue

                val sameType = current.type == candidate.type
                val sameAmount = Math.abs(current.amount - candidate.amount) < 0.01
                val withinWindow = Math.abs(current.timestamp - candidate.timestamp) <= DEDUPLICATION_WINDOW_MS

                if (sameType && sameAmount && withinWindow) {
                    val sameRawText = current.rawText.trim().equals(candidate.rawText.trim(), ignoreCase = true)
                    val relatedMerchants = areMerchantsMatchingOrRelated(current.merchant, candidate.merchant)
                    val bothRefunds = current.rawText.lowercase().contains("refund") && candidate.rawText.lowercase().contains("refund")
                    val bothCashback = current.rawText.lowercase().contains("cashback") && candidate.rawText.lowercase().contains("cashback")

                    if (sameRawText || relatedMerchants || bothRefunds || bothCashback) {
                        toDelete.add(candidate)
                    }
                }
            }
        }

        var deletedCount = 0
        for (duplicate in toDelete) {
            dao.deleteTransaction(duplicate)
            deletedCount++
        }

        return deletedCount
    }

    private fun areMerchantsMatchingOrRelated(m1: String, m2: String): Boolean {
        val norm1 = normalizeMerchant(m1)
        val norm2 = normalizeMerchant(m2)

        if (norm1 == norm2) return true
        if (isGenericMerchant(m1) || isGenericMerchant(m2)) return true

        return isBrandMatch(m1, m2)
    }

    private fun isBrandMatch(m1: String, m2: String): Boolean {
        val norm1 = normalizeMerchant(m1)
        val norm2 = normalizeMerchant(m2)

        if (norm1.length >= 3 && norm2.length >= 3) {
            if (norm1.contains(norm2) || norm2.contains(norm1)) return true
        }
        return false
    }

    private fun normalizeMerchant(merchant: String): String {
        return merchant.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
    }

    private fun isGenericMerchant(merchant: String): Boolean {
        val lower = merchant.lowercase().trim()
        return lower == "bank transaction" || lower == "deposit / transfer" || lower == "online payment" || lower == "merchant payment" || lower.isBlank()
    }
}
