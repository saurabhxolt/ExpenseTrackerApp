package com.expensetracker.app.ingestion.parser

import java.util.regex.Pattern

object RegexTransactionParser {

    private val OTP_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(otp|verification code|one time password|secret code)\\b"),
        Pattern.compile("(?i)\\buse code\\b")
    )

    private val EXCLUSION_PATTERNS = listOf(
        Pattern.compile("(?i)will be deducted"),
        Pattern.compile("(?i)will be debited"),
        Pattern.compile("(?i)available bal.*is inr"),
        Pattern.compile("(?i)available bal.*is rs"),
        Pattern.compile("(?i)for updated a/c bal"),
        Pattern.compile("(?i)cheques are subject to clearing"),
        Pattern.compile("(?i)e-mandate"),
        Pattern.compile("(?i)due on"),
        Pattern.compile("(?i)reminder to pay"),
        Pattern.compile("(?i)statement generated"),
        Pattern.compile("(?i)reward points")
    )

    // Debit action triggers - mandatory currency symbol or explicit debit phrase
    private val DEBIT_PATTERNS = listOf(
        Pattern.compile("(?i)debited\\s+by\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)debited\\s+for\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)debited\\s+with\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:has\\s+been|was|is)?\\s*debited"),
        Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:is|was)?\\s*spent"),
        Pattern.compile("(?i)spent\\s+using\\s+.*?\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)spent\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)paid\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)sent\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)withdrawn\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)deducted\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)txn\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)dr\\s+of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)purchase\\s+of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    )

    // Credit action triggers - mandatory currency symbol or explicit credit phrase
    private val CREDIT_PATTERNS = listOf(
        Pattern.compile("(?i)has\\s+credit\\s+for\\s+.*?of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)credited\\s+by\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)credited\\s+with\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)credited\\s+to\\s+.*?\\s*(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:has\\s+been|was|is)?\\s*credited"),
        Pattern.compile("(?i)refund\\s+of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)cashback\\s+of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)salary\\s+of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)received\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)deposited\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)added\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)cr\\s+of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    )

    fun parse(text: String): ParsedTransaction? {
        if (text.isBlank()) return null

        // 1. Reject OTPs
        for (pattern in OTP_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return null
            }
        }

        val cleanText = text.replace("\n", " ").trim()

        // 2. Always check EXCLUSION_PATTERNS first to reject upcoming auto-debits, mandates & balance alerts
        for (pattern in EXCLUSION_PATTERNS) {
            if (pattern.matcher(cleanText).find()) {
                return null
            }
        }

        // 3. Pre-process cleanText: remove balance and limit suffix clauses so we don't match balance amounts
        val textWithoutBalance = stripBalanceAndLimitClauses(cleanText)

        // 3.1 Check Credit Card Bill Payment confirmation
        if (isCreditCardBillPayment(cleanText)) {
            val amount = extractGenericAmount(textWithoutBalance)
            if (amount > 0.0) {
                val accountDigits = extractAccountDigits(cleanText)
                return ParsedTransaction(
                    amount = amount,
                    type = "DEBIT",
                    merchant = "Credit Card Bill",
                    accountDigits = accountDigits,
                    category = "Bills & Utilities",
                    rawText = text
                )
            }
        }

        // 3.2 Check Self Transfer
        if (isSelfTransfer(cleanText)) {
            val amount = extractGenericAmount(textWithoutBalance)
            if (amount > 0.0) {
                val accountDigits = extractAccountDigits(cleanText)
                return ParsedTransaction(
                    amount = amount,
                    type = "TRANSFER",
                    merchant = "Self Transfer",
                    accountDigits = accountDigits,
                    category = "Transfer",
                    rawText = text
                )
            }
        }

        // 4. Check DEBIT patterns first
        for (pattern in DEBIT_PATTERNS) {
            val matcher = pattern.matcher(textWithoutBalance)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")
                val amount = amountStr?.toDoubleOrNull() ?: 0.0
                if (amount > 0.0) {
                    val merchant = extractMerchant(cleanText, "DEBIT")
                    val category = predictCategory(merchant, cleanText, "DEBIT")
                    val accountDigits = extractAccountDigits(cleanText)
                    return ParsedTransaction(
                        amount = amount,
                        type = "DEBIT",
                        merchant = merchant,
                        accountDigits = accountDigits,
                        category = category,
                        rawText = text
                    )
                }
            }
        }

        // 5. Check CREDIT patterns next
        for (pattern in CREDIT_PATTERNS) {
            val matcher = pattern.matcher(textWithoutBalance)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")
                val amount = amountStr?.toDoubleOrNull() ?: 0.0
                if (amount > 0.0) {
                    val merchant = extractMerchant(cleanText, "CREDIT")
                    val category = predictCategory(merchant, cleanText, "CREDIT")
                    val accountDigits = extractAccountDigits(cleanText)
                    return ParsedTransaction(
                        amount = amount,
                        type = "CREDIT",
                        merchant = merchant,
                        accountDigits = accountDigits,
                        category = category,
                        rawText = text
                    )
                }
            }
        }

        // 6. Generic Fallback: Match amount + explicit transaction verb if present
        if (containsTransactionKeyword(cleanText)) {
            val genericAmount = extractGenericAmount(textWithoutBalance)
            if (genericAmount > 0.0) {
                val type = if (cleanText.contains("credit", ignoreCase = true) || cleanText.contains("received", ignoreCase = true) || cleanText.contains("refund", ignoreCase = true)) "CREDIT" else "DEBIT"
                val merchant = extractMerchant(cleanText, type)
                val category = predictCategory(merchant, cleanText, type)
                val accountDigits = extractAccountDigits(cleanText)
                return ParsedTransaction(
                    amount = genericAmount,
                    type = type,
                    merchant = merchant,
                    accountDigits = accountDigits,
                    category = category,
                    rawText = text
                )
            }
        }

        return null
    }

    private fun containsTransactionKeyword(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("debited") || lower.contains("spent") || lower.contains("paid") ||
               lower.contains("sent") || lower.contains("withdrawn") || lower.contains("deducted") ||
               lower.contains("credited") || lower.contains("received") || lower.contains("deposited") ||
               lower.contains("refund") || lower.contains("cashback") || lower.contains("txn") ||
               lower.contains("dr of") || lower.contains("cr of") || lower.contains("purchase")
    }

    private fun stripBalanceAndLimitClauses(text: String): String {
        return text
            .replace(Regex("(?i)\\b(available\\s+bal|avl\\s+bal|available\\s+limit|avl\\s+limit|current\\s+outstanding|clear\\s+bal|bal\\s+is).*"), "")
            .replace(Regex("(?i)\\.\\s*avl\\s+limit.*"), "")
            .replace(Regex("(?i)\\.\\s*available\\s+credit.*"), "")
            .trim()
    }

    private fun extractGenericAmount(text: String): Double {
        val pattern = Pattern.compile("(?i)(?:rs\\.?|inr|₹|amt|amount|usd|\\$)\\s*:?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }
        val patternSuffix = Pattern.compile("(?i)([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr|₹|usd|\\$)")
        val matcherSuffix = patternSuffix.matcher(text)
        if (matcherSuffix.find()) {
            return matcherSuffix.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun extractMerchant(text: String, type: String): String {
        // Strip security warning clauses first so "Not You? Call 1800.../SMS BLOCK... to 7308080808" does not match "to <Payee>"
        val cleanText = text
            .replace(Regex("(?i)(?:not\\s+you\\?|if\\s+not\\s+you|call|sms\\s+block).*"), "")
            .replace(Regex("(?i)\\s+for\\s+updated\\s+a/c.*"), "")
            .trim()

        // Pattern 1: trf to <Merchant>
        val trfTo = Pattern.compile("(?i)trf\\s+to\\s+(.*?)(?:\\s+refno|\\s+if|\\s+on|\\.|$)").matcher(cleanText)
        if (trfTo.find()) return cleanMerchantName(trfTo.group(1) ?: "")

        // Pattern 2: on <Date> on <Merchant> (ICICI Card XX9000 on 22-Jul-26 on AMAZON PAY)
        val onDateOnMerchant = Pattern.compile("(?i)on\\s+[0-9]{1,2}-[A-Za-z0-9\\-]+\\s+on\\s+([A-Za-z0-9._\\-@\\s]+?)(?:\\.\\s*avl|\\.\\s*if|\\.|$)").matcher(cleanText)
        if (onDateOnMerchant.find()) return cleanMerchantName(onDateOnMerchant.group(1) ?: "")

        // Pattern 3: at <Merchant>
        val atMerchant = Pattern.compile("(?i)\\s+at\\s+(.*?)(?:\\s+on\\s+[0-9]{2}-[0-9]{2}|\\s+by\\s+upi|\\.\\s*available|\\.\\s*avl|\\.|$)").matcher(cleanText)
        if (atMerchant.find()) return cleanMerchantName(atMerchant.group(1) ?: "")

        // Pattern 4: towards <Merchant>
        val towardsMerchant = Pattern.compile("(?i)towards\\s+(.*?)(?:\\s+via|\\s+on|\\s+vide|\\.|$)").matcher(cleanText)
        if (towardsMerchant.find()) return cleanMerchantName(towardsMerchant.group(1) ?: "")

        // Pattern 5: for <Merchant> order/recharge/bill
        val forMerchant = Pattern.compile("(?i)\\s+for\\s+([A-Za-z0-9._\\-@\\s]+?)(?:\\s+order|\\s+recharge|\\s+bill|\\s+on|\\.|$)").matcher(cleanText)
        if (forMerchant.find()) {
            val merchantCandidate = forMerchant.group(1) ?: ""
            if (!merchantCandidate.lowercase().contains("iccl mutual") && merchantCandidate.isNotBlank()) {
                return cleanMerchantName(merchantCandidate)
            }
        }

        // Pattern 6: to <Payee> (excluding phone numbers)
        val toPayee = Pattern.compile("(?i)\\s+to\\s+([A-Za-z\\s@._\\-]+?)(?:\\s+on\\s+|\\s+via\\s+|\\s+ref|\\.|$)").matcher(cleanText)
        if (toPayee.find()) {
            val candidate = toPayee.group(1) ?: ""
            if (!candidate.trim().matches(Regex("^[0-9\\s+\\-]+$"))) {
                return cleanMerchantName(candidate)
            }
        }

        // Pattern 7: from <Sender>
        val fromSender = Pattern.compile("(?i)from\\s+(.*?)(?:\\s+via|\\s+on|\\s+ref|\\.|$)").matcher(cleanText)
        if (fromSender.find()) return cleanMerchantName(fromSender.group(1) ?: "")

        return if (type == "CREDIT") "Deposit / Transfer" else "Bank Transaction"
    }

    private fun extractAccountDigits(text: String): String {
        val pattern = Pattern.compile("(?i)(?:a/c|acct|card|account)\\s*(?:ending|no|#)?\\s*x*([0-9]{3,4})")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1) ?: ""
        }
        return ""
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
            .replace(Regex("(?i)\\s+ref\\s+no.*"), "")

        if (clean.length > 25) {
            clean = clean.substring(0, 25).trim()
        }

        return if (clean.isBlank()) "Bank Transaction" else clean
    }

    private fun predictCategory(merchant: String, rawText: String, type: String): String {
        if (type == "CREDIT") return "Income"

        val lower = "$merchant $rawText".lowercase()
        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("cafe") || lower.contains("atithi") || lower.contains("restaurant") || lower.contains("bakery") || lower.contains("dining") || lower.contains("dominos") || lower.contains("mcdonald") || lower.contains("starbucks") || lower.contains("kfc") -> "Food & Dining"
            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("irctc") || lower.contains("fuel") || lower.contains("petrol") || lower.contains("fastag") || lower.contains("toll") || lower.contains("railway") || lower.contains("metro") -> "Transportation"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("ajio") || lower.contains("dmart") || lower.contains("blinkit") || lower.contains("zepto") || lower.contains("bigbasket") || lower.contains("mart") || lower.contains("store") || lower.contains("retail") -> "Shopping"
            lower.contains("airtel") || lower.contains("jio") || lower.contains("vi ") || lower.contains("bill") || lower.contains("recharge") || lower.contains("bescom") || lower.contains("electricity") || lower.contains("water") || lower.contains("gas") || lower.contains("broadband") || lower.contains("tata play") || lower.contains("dth") -> "Bills & Utilities"
            lower.contains("ksec") || lower.contains("zerodha") || lower.contains("groww") || lower.contains("upstox") || lower.contains("trading") || lower.contains("mutual fund") || lower.contains("sip") || lower.contains("coin") || lower.contains("stock") -> "Investments"
            lower.contains("netflix") || lower.contains("prime video") || lower.contains("spotify") || lower.contains("youtube") || lower.contains("bookmyshow") || lower.contains("pvr") || lower.contains("inox") || lower.contains("hotstar") || lower.contains("cinema") -> "Entertainment"
            lower.contains("pharmacy") || lower.contains("apollo") || lower.contains("practo") || lower.contains("hospital") || lower.contains("diagnostic") || lower.contains("medical") || lower.contains("pharmeasy") || lower.contains("1mg") || lower.contains("clinic") -> "Health & Medical"
            else -> "General Expense"
        }
    }

    private fun isCreditCardBillPayment(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.contains("credit card") || lower.contains("bobcard") || lower.contains("card xx") || lower.contains("card ending")) &&
               (lower.contains("payment received") || lower.contains("credited towards") || lower.contains("payment of") || lower.contains("bill payment") || lower.contains("received towards") || lower.contains("thank you for paying"))
    }

    private fun isSelfTransfer(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("self transfer") || lower.contains("own account") ||
               (lower.contains("transferred to your a/c") && lower.contains("from a/c"))
    }
}


