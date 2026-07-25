package com.expensetracker.app.ingestion.parser

import java.util.regex.Pattern

object RegexTransactionParser {

    private val EXCLUSION_PATTERNS = listOf(
        Pattern.compile("(?i)will be deducted"),
        Pattern.compile("(?i)will be debited"),
        Pattern.compile("(?i)available bal.*is inr"),
        Pattern.compile("(?i)available bal.*is rs"),
        Pattern.compile("(?i)for updated a/c bal"),
        Pattern.compile("(?i)cheques are subject to clearing"),
        Pattern.compile("(?i)e-mandate"),
        Pattern.compile("(?i)due on")
    )

    fun parse(text: String): ParsedTransaction? {
        if (text.isBlank()) return null

        for (pattern in EXCLUSION_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return null
            }
        }

        val cleanText = text.replace("\n", " ").trim()

        // 1. SBI UPI Debit: "debited by 60.00 on date 24Jul26 trf to KRISHNA KUMAR Refno 806766285828"
        val sbiDebit = Pattern.compile("(?i)debited\\s+by\\s+([0-9,]+(?:\\.[0-9]{1,2})?)\\s+on\\s+date\\s+.*?trf\\s+to\\s+(.*?)(?:\\s+refno|\\s+if|$)").matcher(cleanText)
        if (sbiDebit.find()) {
            val amount = sbiDebit.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val merchant = cleanMerchantName(sbiDebit.group(2) ?: "SBI Transfer")
            return ParsedTransaction(amount = amount, type = "DEBIT", merchant = merchant, category = predictCategory(merchant, cleanText), rawText = text)
        }

        // 2. HDFC Txn: "Txn Rs.938.70 On HDFC Bank Card 7478 At SV2512112238344230219611@ by UPI..."
        val hdfcTxn = Pattern.compile("(?i)txn\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+on\\s+.*?at\\s+(.*?)(?:\\s+by\\s+upi|\\s+on\\s+[0-9]{2}-[0-9]{2}|\\s+not|$)").matcher(cleanText)
        if (hdfcTxn.find()) {
            val amount = hdfcTxn.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val merchant = cleanMerchantName(hdfcTxn.group(2) ?: "HDFC Merchant")
            return ParsedTransaction(amount = amount, type = "DEBIT", merchant = merchant, category = predictCategory(merchant, cleanText), rawText = text)
        }

        // 3. BOBCARD Spent: "ALERT: INR 755.00 is spent on your BOBCARD ending 1158 at Upi-m S Atithi Vaibhav on 24-07-2026..."
        val bobCard = Pattern.compile("(?i)(?:inr|rs\\.?|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+is\\s+spent\\s+on\\s+.*?at\\s+(.*?)(?:\\s+on\\s+[0-9]{2}-[0-9]{2}|\\.\\s*available|\\.|$)").matcher(cleanText)
        if (bobCard.find()) {
            val amount = bobCard.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val merchant = cleanMerchantName(bobCard.group(2) ?: "BOBCARD Merchant")
            return ParsedTransaction(amount = amount, type = "DEBIT", merchant = merchant, category = predictCategory(merchant, cleanText), rawText = text)
        }

        // 4. ICICI Spent: "INR 355.11 spent using ICICI Bank Card XX9000 on 22-Jul-26 on AMAZON PAY WALL. Avl Limit..."
        val iciciSpent = Pattern.compile("(?i)(?:inr|rs\\.?|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+spent\\s+using\\s+.*?on\\s+[A-Za-z0-9\\-]+\\s+on\\s+([A-Za-z0-9._\\-@\\s]+?)(?:\\.\\s*avl|\\.\\s*if|$)").matcher(cleanText)
        if (iciciSpent.find()) {
            val amount = iciciSpent.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val merchant = cleanMerchantName(iciciSpent.group(2) ?: "ICICI Merchant")
            return ParsedTransaction(amount = amount, type = "DEBIT", merchant = merchant, category = predictCategory(merchant, cleanText), rawText = text)
        }

        // 5. Kotak Debited: "Rs.672.00 has been debited from your Kotak Bank A/c XX8107 towards KSec trading account..."
        val kotakDebit = Pattern.compile("(?i)(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+has\\s+been\\s+debited\\s+from\\s+.*?towards\\s+(.*?)(?:\\s+via|\\s+on|\\s+vide|$)").matcher(cleanText)
        if (kotakDebit.find()) {
            val amount = kotakDebit.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            val merchant = cleanMerchantName(kotakDebit.group(2) ?: "Kotak Merchant")
            return ParsedTransaction(amount = amount, type = "DEBIT", merchant = merchant, category = predictCategory(merchant, cleanText), rawText = text)
        }

        // 6. SBI Credit: "Your A/C XXXXX819477 has credit for VREF ... of Rs 66.00 on 16/07/26. Avl Bal..."
        val sbiCredit = Pattern.compile("(?i)has\\s+credit\\s+for\\s+.*?of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)").matcher(cleanText)
        if (sbiCredit.find()) {
            val amount = sbiCredit.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            return ParsedTransaction(amount = amount, type = "CREDIT", merchant = "Deposit / Transfer", category = "Income", rawText = text)
        }

        return null
    }

    private fun cleanMerchantName(raw: String): String {
        var clean = raw.trim()

        if (clean.contains("@")) {
            val parts = clean.split("@")[0].trim()
            clean = when {
                parts.contains("airtel", ignoreCase = true) -> "Airtel"
                parts.contains("swiggy", ignoreCase = true) -> "Swiggy"
                parts.contains("zomato", ignoreCase = true) -> "Zomato"
                parts.contains("uber", ignoreCase = true) -> "Uber"
                parts.contains("paytm", ignoreCase = true) -> "Paytm"
                parts.startsWith("upi-m", ignoreCase = true) -> parts.substring(5).trim()
                parts.length > 15 -> parts
                else -> parts
            }
        }

        if (clean.startsWith("Upi-m ", ignoreCase = true)) {
            clean = clean.substring(6).trim()
        }

        clean = clean
            .replace(Regex("(?i)\\s+by\\s+upi.*"), "")
            .replace(Regex("(?i)\\s+on\\s+[0-9]{2}-[0-9]{2}.*"), "")
            .replace(Regex("(?i)\\s+refno.*"), "")

        if (clean.length > 25) {
            clean = clean.substring(0, 25).trim()
        }

        return if (clean.isBlank()) "Bank Transaction" else clean
    }

    private fun predictCategory(merchant: String, rawText: String): String {
        val lower = "$merchant $rawText".lowercase()
        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("cafe") || lower.contains("atithi vaibhav") || lower.contains("restaurant") -> "Food & Dining"
            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("irctc") || lower.contains("fuel") || lower.contains("petrol") -> "Transportation"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("mart") || lower.contains("store") -> "Shopping"
            lower.contains("airtel") || lower.contains("jio") || lower.contains("bill") || lower.contains("recharge") || lower.contains("bescom") || lower.contains("electricity") -> "Bills & Utilities"
            lower.contains("ksec") || lower.contains("zerodha") || lower.contains("groww") || lower.contains("trading") || lower.contains("mutual fund") -> "Investments"
            else -> "General Expense"
        }
    }
}
