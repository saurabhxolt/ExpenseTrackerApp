package com.expensetracker.app.ingestion.parser

import java.util.regex.Pattern

object RegexTransactionParser {

    private val DEBIT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:debited|spent|paid|sent)\\s+(?:by|for|rs\\.?|inr)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)rs\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:debited|spent|paid)"),
        Pattern.compile("(?i)vpa\\s+([a-zA-Z0-9._-]+@[a-zA-Z0-9]+)\\s+debited\\s+by\\s+rs\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    )

    private val CREDIT_PATTERNS = listOf(
        Pattern.compile("(?i)(?:credited|received|added)\\s+(?:by|for|rs\\.?|inr)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)rs\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:credited|received)")
    )

    private val MERCHANT_PATTERN = Pattern.compile("(?i)(?:at|to|vpa|info)\\s+([A-Za-z0-9._\\-@\\s]{3,25})")

    fun parse(text: String): ParsedTransaction? {
        if (text.isBlank()) return null

        val cleanText = text.replace(",", "")

        // Check Debit
        for (pattern in DEBIT_PATTERNS) {
            val matcher = pattern.matcher(cleanText)
            if (matcher.find()) {
                val amountStr = matcher.group(matcher.groupCount()) ?: continue
                val amount = amountStr.toDoubleOrNull() ?: continue
                val merchant = extractMerchant(cleanText)
                val category = predictCategory(merchant)
                return ParsedTransaction(
                    amount = amount,
                    type = "DEBIT",
                    merchant = merchant,
                    category = category,
                    rawText = text
                )
            }
        }

        // Check Credit
        for (pattern in CREDIT_PATTERNS) {
            val matcher = pattern.matcher(cleanText)
            if (matcher.find()) {
                val amountStr = matcher.group(matcher.groupCount()) ?: continue
                val amount = amountStr.toDoubleOrNull() ?: continue
                val merchant = extractMerchant(cleanText)
                return ParsedTransaction(
                    amount = amount,
                    type = "CREDIT",
                    merchant = merchant,
                    category = "Income",
                    rawText = text
                )
            }
        }

        return null
    }

    private fun extractMerchant(text: String): String {
        val matcher = MERCHANT_PATTERN.matcher(text)
        if (matcher.find()) {
            val candidate = matcher.group(1)?.trim() ?: ""
            if (candidate.isNotBlank() && !candidate.contains("ref", true)) {
                return candidate
            }
        }
        return "Unknown Merchant"
    }

    private fun predictCategory(merchant: String): String {
        val lower = merchant.lowercase()
        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("cafe") || lower.contains("restaurant") -> "Food & Dining"
            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("irctc") || lower.contains("fuel") || lower.contains("petrol") -> "Transportation"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("mart") -> "Shopping"
            lower.contains("bill") || lower.contains("recharge") || lower.contains("bescom") || lower.contains("electricity") -> "Bills & Utilities"
            else -> "General Expense"
        }
    }
}
