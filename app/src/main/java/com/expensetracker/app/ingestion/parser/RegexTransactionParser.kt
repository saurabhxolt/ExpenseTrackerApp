package com.expensetracker.app.ingestion.parser

import java.util.regex.Pattern

object RegexTransactionParser {

    private val DEBIT_KEYWORDS = listOf(
        "debited", "spent", "paid", "sent", "transferred", "withdrawn", "purchase", "txn of", "paid to"
    )
    private val CREDIT_KEYWORDS = listOf(
        "credited", "received", "added", "deposited", "refund"
    )

    private val AMOUNT_PATTERN = Pattern.compile("(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val REVERSE_AMOUNT_PATTERN = Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr|₹)", Pattern.CASE_INSENSITIVE)

    fun parse(text: String): ParsedTransaction? {
        if (text.isBlank()) return null
        val lowerText = text.lowercase()

        // Extract amount
        var amount: Double? = null
        val amountMatcher = AMOUNT_PATTERN.matcher(text)
        if (amountMatcher.find()) {
            amount = amountMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        } else {
            val revMatcher = REVERSE_AMOUNT_PATTERN.matcher(text)
            if (revMatcher.find()) {
                amount = revMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()
            }
        }

        if (amount == null || amount <= 0) return null

        // Determine Debit vs Credit
        val isCredit = CREDIT_KEYWORDS.any { lowerText.contains(it) }
        val isDebit = DEBIT_KEYWORDS.any { lowerText.contains(it) } || !isCredit

        val type = if (isCredit) "CREDIT" else "DEBIT"
        val merchant = extractMerchant(text)
        val category = if (isCredit) "Income" else predictCategory(merchant)

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = merchant,
            category = category,
            rawText = text
        )
    }

    private fun extractMerchant(text: String): String {
        val patterns = listOf(
            Pattern.compile("(?:paid to|sent to|at|to|vpa|via|for)\\s+([A-Za-z0-9._\\-@\\s]{2,30})", Pattern.CASE_INSENSITIVE)
        )
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim() ?: ""
                val cleanCandidate = candidate.split(" on ", " using ", " ref ", " avl ", " bal ", " A/C ", " a/c ")[0].trim()
                if (cleanCandidate.isNotBlank() && cleanCandidate.length <= 25) {
                    return cleanCandidate
                }
            }
        }
        return "Bank Transaction"
    }

    private fun predictCategory(merchant: String): String {
        val lower = merchant.lowercase()
        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("cafe") || lower.contains("restaurant") -> "Food & Dining"
            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("irctc") || lower.contains("fuel") || lower.contains("petrol") -> "Transportation"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("mart") || lower.contains("store") -> "Shopping"
            lower.contains("bill") || lower.contains("recharge") || lower.contains("bescom") || lower.contains("electricity") || lower.contains("airtel") || lower.contains("jio") -> "Bills & Utilities"
            else -> "General Expense"
        }
    }
}
