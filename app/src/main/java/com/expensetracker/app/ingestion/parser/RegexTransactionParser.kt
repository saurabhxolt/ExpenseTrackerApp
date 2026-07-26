package com.expensetracker.app.ingestion.parser

import java.util.regex.Pattern

object RegexTransactionParser {

    private val OTP_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(otp|verification code|one time password|secret code)\\b"),
        Pattern.compile("(?i)\\buse code\\b")
    )

    private val PROMOTIONAL_OFFER_PATTERNS = listOf(
        Pattern.compile("(?i)require\\s+consent"),
        Pattern.compile("(?i)disbursement"),
        Pattern.compile("(?i)funds.*are\\s+available"),
        Pattern.compile("(?i)service\\s+alert:"),
        Pattern.compile("(?i)eligible\\s+for"),
        Pattern.compile("(?i)pre-approved"),
        Pattern.compile("(?i)apply\\s+now"),
        Pattern.compile("(?i)upgrade\\s+your\\s+limit"),
        Pattern.compile("(?i)instant\\s+loan")
    )

    private val INFORMATIONAL_STATEMENT_PATTERNS = listOf(
        Pattern.compile("(?i)statement\\s+is\\s+sent"),
        Pattern.compile("(?i)statement\\s+generated"),
        Pattern.compile("(?i)statement\\s+of\\s+your\\s+card"),
        Pattern.compile("(?i)minimum.*is\\s+due"),
        Pattern.compile("(?i)total\\s+of.*is\\s+due"),
        Pattern.compile("(?i)total\\s+amount\\s+due"),
        Pattern.compile("(?i)will\\s+be\\s+deducted"),
        Pattern.compile("(?i)will\\s+be\\s+debited"),
        Pattern.compile("(?i)has\\s+been\\s+initiated"),
        Pattern.compile("(?i)refund.*initiated"),
        Pattern.compile("(?i)available\\s+bal.*is\\s+inr"),
        Pattern.compile("(?i)available\\s+bal.*is\\s+rs"),
        Pattern.compile("(?i)for\\s+updated\\s+a/c\\s+bal"),
        Pattern.compile("(?i)cheques\\s+are\\s+subject\\s+to\\s+clearing"),
        Pattern.compile("(?i)e-mandate"),
        Pattern.compile("(?i)due\\s+on"),
        Pattern.compile("(?i)reminder\\s+to\\s+pay"),
        Pattern.compile("(?i)reward\\s+points")
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

    // Credit action triggers
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

        // 1. STAGE 1: NON-TRANSACTION REJECTION FILTER
        for (pattern in OTP_PATTERNS) {
            if (pattern.matcher(text).find()) return null
        }
        for (pattern in PROMOTIONAL_OFFER_PATTERNS) {
            if (pattern.matcher(text).find()) return null
        }
        val cleanText = text.replace("\n", " ").trim()
        for (pattern in INFORMATIONAL_STATEMENT_PATTERNS) {
            if (pattern.matcher(cleanText).find()) return null
        }

        // 2. STAGE 2: TEXT SANITIZATION & PRE-PROCESSING
        val textWithoutBalance = stripBalanceAndLimitClauses(cleanText)

        // 3. STAGE 3: SEMANTIC ACTION CLASSIFICATION

        // 3.1 Check Merchant/Service Payment Receipts (e.g., Insurance, MoRTH Fee, Order Receipts)
        if (isPaymentReceiptForExpense(cleanText)) {
            val amount = extractGenericAmount(textWithoutBalance)
            if (amount > 0.0) {
                val merchant = extractMerchant(cleanText, "DEBIT")
                val accountDigits = extractAccountDigits(cleanText)
                val category = if (cleanText.lowercase().contains("insurance") || cleanText.lowercase().contains("covered")) "Insurance"
                               else if (cleanText.lowercase().contains("fee") || cleanText.lowercase().contains("morth") || cleanText.lowercase().contains("registration")) "Government & Fees"
                               else predictCategory(merchant, cleanText, "DEBIT")
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

        // 3.2 Check Credit Card Bill Payment confirmation
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

        // 3.3 Check Self Transfer
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

        // 3.4 Check DEBIT patterns
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

        // 3.5 Check CREDIT patterns (Genuine External Income)
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

        // 3.6 Generic Fallback
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

    private fun isPaymentReceiptForExpense(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.contains("received payment") || lower.contains("received a payment") || lower.contains("received rs") || lower.contains("received inr") || lower.contains("payment received") || lower.contains("payment of")) &&
               (lower.contains("for your") || lower.contains("against") || lower.contains("fee") || lower.contains("insurance") || lower.contains("morth") || lower.contains("policy") || lower.contains("acko") || lower.contains("receipt no") || lower.contains("airtel") || lower.contains("bill"))
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
        val lowerFull = text.lowercase()
        if (lowerFull.contains("airtel black")) return "Airtel Black"
        if (lowerFull.contains("airtel wi-fi") || lowerFull.contains("airtel wifi")) return "Airtel Wi-Fi"
        if (lowerFull.contains("zepto cash")) return "Zepto Cash"
        if (lowerFull.contains("zepto")) return "Zepto"
        if (lowerFull.contains("acko")) return "ACKO Insurance"
        if (lowerFull.contains("morth")) return "MoRTH"

        // Strip security warnings AND HH:MM:SS timestamps so "at 02:40:16" does not become merchant!
        val cleanText = text
            .replace(Regex("(?i)(?:not\\s+you\\?|if\\s+not\\s+you|call|sms\\s+block).*"), "")
            .replace(Regex("(?i)\\s+for\\s+updated\\s+a/c.*"), "")
            .replace(Regex("(?i)\\s+at\\s+[0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?.*"), "")
            .trim()

        // Pattern 1: credited to <Name> / transferred to <Name> (NEFT/UPI Beneficiary)
        val creditedTo = Pattern.compile("(?i)(?:credited|transferred)\\s+to\\s+([A-Za-z\\s]+?)(?:\\s+on|\\s+at|\\.|$)").matcher(cleanText)
        if (creditedTo.find()) {
            val name = creditedTo.group(1)?.trim() ?: ""
            if (name.length in 3..35 && !name.lowercase().contains("account")) {
                return cleanMerchantName(name)
            }
        }

        // Pattern 2: trf to <Merchant>
        val trfTo = Pattern.compile("(?i)trf\\s+to\\s+(.*?)(?:\\s+refno|\\s+if|\\s+on|\\.|$)").matcher(cleanText)
        if (trfTo.find()) return cleanMerchantName(trfTo.group(1) ?: "")

        // Pattern 3: on <Date> on <Merchant> (ICICI Card XX9000 on 22-Jul-26 on AMAZON PAY)
        val onDateOnMerchant = Pattern.compile("(?i)on\\s+[0-9]{1,2}-[A-Za-z0-9\\-]+\\s+on\\s+([A-Za-z0-9._\\-@\\s]+?)(?:\\.\\s*avl|\\.\\s*if|\\.|$)").matcher(cleanText)
        if (onDateOnMerchant.find()) return cleanMerchantName(onDateOnMerchant.group(1) ?: "")

        // Pattern 4: at <Merchant> (excluding timestamps)
        val atMerchant = Pattern.compile("(?i)\\s+at\\s+([A-Za-z0-9._\\-@\\s]+?)(?:\\s+on\\s+[0-9]{2}-[0-9]{2}|\\s+by\\s+upi|\\.\\s*available|\\.\\s*avl|\\.|$)").matcher(cleanText)
        if (atMerchant.find()) {
            val candidate = atMerchant.group(1) ?: ""
            if (!candidate.trim().matches(Regex("^[0-9:]+$"))) {
                return cleanMerchantName(candidate)
            }
        }

        // Pattern 5: towards <Merchant>
        val towardsMerchant = Pattern.compile("(?i)towards\\s+(.*?)(?:\\s+via|\\s+on|\\s+vide|\\.|$)").matcher(cleanText)
        if (towardsMerchant.find()) return cleanMerchantName(towardsMerchant.group(1) ?: "")

        // Pattern 6: for <Merchant> order/recharge/bill/insurance/fee
        val forMerchant = Pattern.compile("(?i)\\s+for\\s+([A-Za-z0-9._\\-@\\s]+?)(?:\\s+order|\\s+recharge|\\s+bill|\\s+insurance|\\s+fee|\\s+on|\\.|$)").matcher(cleanText)
        if (forMerchant.find()) {
            val merchantCandidate = forMerchant.group(1) ?: ""
            if (!merchantCandidate.lowercase().contains("iccl mutual") && merchantCandidate.isNotBlank()) {
                return cleanMerchantName(merchantCandidate)
            }
        }

        // Pattern 7: against <Merchant/Fee>
        val againstMerchant = Pattern.compile("(?i)against\\s+([A-Za-z0-9._\\-@\\s]+?)(?:\\s+vide|\\s+receipt|\\.|$)").matcher(cleanText)
        if (againstMerchant.find()) return cleanMerchantName(againstMerchant.group(1) ?: "")

        // Pattern 8: to <Payee> (excluding phone numbers)
        val toPayee = Pattern.compile("(?i)\\s+to\\s+([A-Za-z\\s@._\\-]+?)(?:\\s+on\\s+|\\s+via\\s+|\\s+ref|\\.|$)").matcher(cleanText)
        if (toPayee.find()) {
            val candidate = toPayee.group(1) ?: ""
            if (!candidate.trim().matches(Regex("^[0-9\\s+\\-]+$"))) {
                return cleanMerchantName(candidate)
            }
        }

        // Pattern 9: from <Sender>
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

        val lowerRaw = raw.lowercase()
        if (lowerRaw.contains("acko")) return "ACKO Insurance"
        if (lowerRaw.contains("morth")) return "MoRTH"

        if (clean.contains("@")) {
            val parts = clean.split("@")[0].trim()
            clean = when {
                parts.contains("airtel", ignoreCase = true) -> "Airtel"
                parts.contains("swiggy", ignoreCase = true) -> "Swiggy"
                parts.contains("zomato", ignoreCase = true) -> "Zomato"
                parts.contains("uber", ignoreCase = true) -> "Uber"
                parts.contains("paytm", ignoreCase = true) -> "Paytm"
                parts.contains("acko", ignoreCase = true) -> "ACKO Insurance"
                parts.startsWith("upi-m", ignoreCase = true) -> parts.substring(5).trim()
                parts.length > 15 -> parts
                else -> parts
            }
        }

        if (clean.startsWith("Upi-m ", ignoreCase = true)) {
            clean = clean.substring(6).trim()
        }
        if (clean.startsWith("your ", ignoreCase = true)) {
            clean = clean.substring(5).trim()
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
            lower.contains("insurance") || lower.contains("acko") || lower.contains("policy") || lower.contains("lic") -> "Insurance"
            lower.contains("morth") || lower.contains("rto") || lower.contains("fee") || lower.contains("registration fee") || lower.contains("tax") -> "Government & Fees"
            else -> "General Expense"
        }
    }
}
