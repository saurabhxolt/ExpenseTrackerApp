package com.expensetracker.app.ingestion.deduplication

import com.expensetracker.app.core.database.dao.TransactionDao
import com.expensetracker.app.core.database.entity.TransactionEntity

object TransactionDeduplicator {

    private val GENERIC_MERCHANT_TOKENS = setOf(
        "bank", "transaction", "payment", "online", "deposit", "transfer", "upi", "vpa",
        "dr", "cr", "inr", "rs", "amt", "amount", "paid", "debited", "credited", "store", "shop", "at"
    )

    const val DEFAULT_DEDUPLICATION_WINDOW_MS = 2 * 60 * 60 * 1000L // 2-hour window for notification/SMS duplicates
    const val CC_PAYMENT_DEDUPLICATION_WINDOW_MS = 4 * 24 * 60 * 60 * 1000L // 4-day window for Credit Card settlements

    suspend fun insertWithDeduplication(dao: TransactionDao, entity: TransactionEntity): Long {
        // 1. Credit Card Payment Deduplication (Window: 4 days)
        val isCreditCardPayment = isCreditCardRelated(entity)

        if (isCreditCardPayment) {
            val candidatePayments = dao.findCreditCardPaymentsInWindow(
                amount = entity.amount,
                timestamp = entity.timestamp,
                windowMs = CC_PAYMENT_DEDUPLICATION_WINDOW_MS
            ).ifEmpty {
                val single = dao.findExistingCreditCardPayment(entity.amount)
                if (single != null && Math.abs(single.timestamp - entity.timestamp) <= CC_PAYMENT_DEDUPLICATION_WINDOW_MS) {
                    listOf(single)
                } else emptyList()
            }

            for (existingCardPayment in candidatePayments) {
                if (existingCardPayment.transactionHash != entity.transactionHash) {
                    if (isGenericMerchant(existingCardPayment.merchant) && !isGenericMerchant(entity.merchant)) {
                        val updated = existingCardPayment.copy(
                            merchant = entity.merchant,
                            category = entity.category
                        )
                        dao.updateTransaction(updated)
                    }
                    return -1L // Suppress duplicate credit card payment confirmation within 4-day window
                }
            }
        }

        // 2. Dual-Channel Candidate Duplicate Search (Window: 2 hours)
        val candidateDuplicates = dao.findDuplicatesInWindow(
            type = entity.type,
            amount = entity.amount,
            timestamp = entity.timestamp,
            windowMs = DEFAULT_DEDUPLICATION_WINDOW_MS
        ).ifEmpty {
            val single = dao.findCandidateDuplicate(type = entity.type, amount = entity.amount)
            if (single != null && Math.abs(single.timestamp - entity.timestamp) <= DEFAULT_DEDUPLICATION_WINDOW_MS) {
                listOf(single)
            } else emptyList()
        }

        for (existing in candidateDuplicates) {
            if (existing.transactionHash == entity.transactionHash) {
                return -1L // Identical transaction hash
            }

            val sameRawText = existing.rawText.trim().equals(entity.rawText.trim(), ignoreCase = true)
            val merchantsMatchOrRelated = areMerchantsMatchingOrRelated(existing.merchant, entity.merchant)
            val bothAreRefunds = existing.rawText.lowercase().contains("refund") && entity.rawText.lowercase().contains("refund")
            val bothAreCashback = existing.rawText.lowercase().contains("cashback") && entity.rawText.lowercase().contains("cashback")
            val bothAreBillPaymentConfirmations = isBillOrPolicyReceipt(existing.rawText) && isBillOrPolicyReceipt(entity.rawText)
            val isExistingGeneric = isGenericMerchant(existing.merchant)
            val isNewGeneric = isGenericMerchant(entity.merchant)

            // Suppress duplicate within window if raw text matches, merchants match/related, both are refunds, both are bill receipts, or one is generic
            if (sameRawText || merchantsMatchOrRelated || bothAreRefunds || bothAreCashback || bothAreBillPaymentConfirmations || isExistingGeneric || isNewGeneric) {
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
                return -1L // Duplicate suppressed within window
            }
        }

        return dao.insertTransaction(entity)
    }

    suspend fun cleanupExistingDuplicates(dao: TransactionDao): Int {
        val allTxns = dao.getAllTransactionsList()
        val toDelete = mutableSetOf<TransactionEntity>()

        for (i in 0 until allTxns.size) {
            val current = allTxns[i]
            if (toDelete.contains(current)) continue

            if (isPromotionalOrInvalid(current)) {
                toDelete.add(current)
                continue
            }

            // Retroactive migration for historical Credit Card Bill Payments logged before update
            if (current.type != "TRANSFER" && isCreditCardPaymentText(current.rawText, current.merchant)) {
                val newMerchant = if (current.merchant.isBlank() || isGenericMerchant(current.merchant) || current.merchant == "Unknown Merchant") {
                    com.expensetracker.app.ingestion.parser.RegexTransactionParser.extractCreditCardMerchant(current.rawText)
                } else current.merchant
                val updated = current.copy(type = "TRANSFER", category = "Bills & Utilities", merchant = newMerchant)
                dao.updateTransaction(updated)
            }

            val isCurrentCC = isCreditCardRelated(current)

            for (j in i + 1 until allTxns.size) {
                val candidate = allTxns[j]
                if (toDelete.contains(candidate)) continue

                val isCandidateCC = isCreditCardRelated(candidate)
                val sameType = current.type == candidate.type
                val sameAmount = Math.abs(current.amount - candidate.amount) < 0.01
                val timeDiff = Math.abs(current.timestamp - candidate.timestamp)

                // 1. Credit Card Settlement Cleanup within 4-day window
                if (isCurrentCC && isCandidateCC && sameAmount && timeDiff <= CC_PAYMENT_DEDUPLICATION_WINDOW_MS) {
                    toDelete.add(candidate)
                    continue
                }

                // 2. Duplicate Cleanup (Only within 2-hour window, or exact same text within 24 hours)
                if (sameType && sameAmount) {
                    val sameRawText = current.rawText.trim().equals(candidate.rawText.trim(), ignoreCase = true)
                    val isWithinWindow = timeDiff <= DEFAULT_DEDUPLICATION_WINDOW_MS
                    val isSameDayExactRawText = sameRawText && timeDiff <= 24 * 60 * 60 * 1000L

                    if (isWithinWindow || isSameDayExactRawText) {
                        val relatedMerchants = areMerchantsMatchingOrRelated(current.merchant, candidate.merchant)
                        val bothRefunds = current.rawText.lowercase().contains("refund") && candidate.rawText.lowercase().contains("refund")
                        val bothCashback = current.rawText.lowercase().contains("cashback") && candidate.rawText.lowercase().contains("cashback")
                        val bothBillReceipts = isBillOrPolicyReceipt(current.rawText) && isBillOrPolicyReceipt(candidate.rawText)

                        if (sameRawText || relatedMerchants || bothRefunds || bothCashback || bothBillReceipts) {
                            toDelete.add(candidate)
                        }
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

    private fun isCreditCardRelated(entity: TransactionEntity): Boolean {
        if (com.expensetracker.app.ingestion.parser.RegexTransactionParser.isCreditCardBillPayment(entity.rawText)) return true
        val lowerMerchant = entity.merchant.lowercase()
        val lowerText = entity.rawText.lowercase()
        return lowerMerchant.contains("credit card") || lowerMerchant.contains("card bill") || lowerMerchant.contains("cheq") || lowerMerchant.contains("cred") || lowerMerchant.contains("billdesk") || lowerMerchant.contains("bbps") ||
               lowerText.contains("credit card") || lowerText.contains("sbi card") || lowerText.contains("bobcard") || lowerText.contains("onecard") || lowerText.contains("cheq") || lowerText.contains("cred") || lowerText.contains("card xx") || lowerText.contains("card ending") || lowerText.contains("billdesk") || lowerText.contains("bbps") ||
               ((entity.type == "DEBIT" || entity.type == "TRANSFER") && entity.category == "Bills & Utilities" && (lowerText.contains("card") || lowerText.contains("bill")))
    }

    private fun isBillOrPolicyReceipt(text: String): Boolean {
        val lower = text.lowercase()
        val hasAction = lower.contains("paid") || lower.contains("debited") || lower.contains("received") || lower.contains("credited") || lower.contains("payment")
        val hasContext = lower.contains("towards") || lower.contains("for your") || lower.contains("bill") || lower.contains("policy") || lower.contains("statement")
        return hasAction && hasContext
    }

    private fun areMerchantsMatchingOrRelated(m1: String, m2: String): Boolean {
        val norm1 = normalizeMerchant(m1)
        val norm2 = normalizeMerchant(m2)

        if (norm1 == norm2) return true
        if (isGenericMerchant(m1) || isGenericMerchant(m2)) return true

        // Generic Brand Token Overlap Matching (Extracts significant brand keywords)
        val tokens1 = extractBrandTokens(m1)
        val tokens2 = extractBrandTokens(m2)

        val commonBrandTokens = tokens1.intersect(tokens2)
        if (commonBrandTokens.isNotEmpty()) return true

        return isBrandMatch(m1, m2)
    }

    private fun extractBrandTokens(merchant: String): Set<String> {
        return merchant.lowercase()
            .split(Regex("[^a-z0-9]"))
            .filter { it.length >= 3 && !GENERIC_MERCHANT_TOKENS.contains(it) }
            .toSet()
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

    private fun isPromotionalOrInvalid(entity: TransactionEntity): Boolean {
        val lowerText = entity.rawText.lowercase()
        return lowerText.contains("up to ₹") || lowerText.contains("up to rs") || lowerText.contains("10% off") ||
               lowerText.contains("no code to hunt") || lowerText.contains("no cashback to chase") ||
               lowerText.contains("what if saving money") || lowerText.contains("scapia federal credit card") ||
               lowerText.contains("apply coupon") || lowerText.contains("use promo code") || lowerText.contains("discount") ||
               lowerText.contains("digest") || lowerText.contains("billion") || lowerText.contains("trillion") ||
               lowerText.contains("cr order") || lowerText.contains("due in") || lowerText.contains("is due") ||
               lowerText.contains("due date") || lowerText.contains("pay instantly") || lowerText.contains("avoid late")
    }

    private fun isGenericMerchant(merchant: String): Boolean {
        val lower = merchant.lowercase().trim()
        return lower == "bank transaction" || lower == "deposit / transfer" || lower == "online payment" || lower == "merchant payment" || lower.isBlank()
    }

    private fun isCreditCardPaymentText(rawText: String, merchant: String): Boolean {
        if (com.expensetracker.app.ingestion.parser.RegexTransactionParser.isCreditCardBillPayment(rawText)) return true
        val lowerText = rawText.lowercase()
        val lowerMerchant = merchant.lowercase()
        return lowerMerchant.contains("credit card bill") || lowerMerchant.contains("card bill") ||
               lowerText.contains("towards your credit card") || lowerText.contains("towards credit card") ||
               lowerText.contains("towards card") || lowerText.contains("cred.club") || lowerText.contains("cheq") ||
               lowerText.contains("cardbill@") || lowerText.contains("ccpay@") || lowerText.contains("billdesk") || lowerText.contains("bbps")
    }
}
