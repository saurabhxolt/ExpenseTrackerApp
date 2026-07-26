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
                // If existing record had generic info and new entity has specific card info, enrich it
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
            val sameMerchant = normalizeMerchant(existing.merchant) == normalizeMerchant(entity.merchant)
            val isExistingGeneric = isGenericMerchant(existing.merchant)
            val isNewGeneric = isGenericMerchant(entity.merchant)

            // Suppress duplicate only if raw text matches, merchants match, or one is generic
            if (sameRawText || sameMerchant || isExistingGeneric || isNewGeneric) {
                if (isExistingGeneric && !isNewGeneric) {
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

    private fun normalizeMerchant(merchant: String): String {
        return merchant.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
    }

    private fun isGenericMerchant(merchant: String): Boolean {
        val lower = merchant.lowercase().trim()
        return lower == "bank transaction" || lower == "deposit / transfer" || lower == "online payment" || lower == "merchant payment" || lower.isBlank()
    }
}
