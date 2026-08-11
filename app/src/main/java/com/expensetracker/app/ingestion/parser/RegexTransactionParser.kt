package com.expensetracker.app.ingestion.parser

import java.util.regex.Pattern

object RegexTransactionParser {

    private val OTP_PATTERNS = listOf(
        Pattern.compile("(?i)\\b(otp|verification code|one time password|secret code|otac)\\b"),
        Pattern.compile("(?i)\\buse code\\b")
    )

    private val FAILED_TRANSACTION_PATTERNS = listOf(
        Pattern.compile("(?i)\\bfailed\\b"),
        Pattern.compile("(?i)\\bunsuccessful\\b"),
        Pattern.compile("(?i)\\bdeclined\\b"),
        Pattern.compile("(?i)payment of .* failed")
    )

    private val PROMOTIONAL_OFFER_PATTERNS = listOf(
        Pattern.compile("(?i)up\\s+to\\s+(?:rs\\.?|inr|₹)"),
        Pattern.compile("(?i)(?:get|save|enjoy|avail)\\s+(?:up\\s+to|flat)?\\s*\\d+%(?:\\s+off)?"),
        Pattern.compile("(?i)\\d+%(?:\\s+off)?"),
        Pattern.compile("(?i)no\\s+code\\s+to\\s+hunt"),
        Pattern.compile("(?i)no\\s+cashback\\s+to\\s+chase"),
        Pattern.compile("(?i)saving\\s+money"),
        Pattern.compile("(?i)what\\s+if"),
        Pattern.compile("(?i)use\\s+promo\\s+code"),
        Pattern.compile("(?i)apply\\s+coupon"),
        Pattern.compile("(?i)require\\s+consent"),
        Pattern.compile("(?i)disbursement"),
        Pattern.compile("(?i)funds.*are\\s+available"),
        Pattern.compile("(?i)funds\\s+ready\\s+for\\s+disbursal"),
        Pattern.compile("(?i)service\\s+alert:"),
        Pattern.compile("(?i)pre-approved"),
        Pattern.compile("(?i)apply\\s+now"),
        Pattern.compile("(?i)upgrade\\s+your\\s+limit"),
        Pattern.compile("(?i)instant\\s+loan"),
        Pattern.compile("(?i)personal\\s+loan\\s+offer"),
        Pattern.compile("(?i)monthly\\s+update"),
        Pattern.compile("(?i)will\\s+expire\\s+on"),
        Pattern.compile("(?i)will\\s+expire\\s+in"),
        Pattern.compile("(?i)premium\\s+starts"),
        Pattern.compile("(?i)reminder\\s+for\\s+appointment"),
        Pattern.compile("(?i)missed\\s+call\\s+from"),
        Pattern.compile("(?i)limit.*cannot\\s+be\\s+increased")
    )

    private val CC_UPI_VPA_PATTERN = Pattern.compile(
        "(?i)\\b[a-z0-9._-]*?(ccpay|cardpay|cardbill|billdesk|bbps|cred|cheq)[a-z0-9._-]*?@[a-z]+\\b"
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
        Pattern.compile("(?i)is\\s+due\\s+on"),
        Pattern.compile("(?i)is\\s+due\\s+for\\s+debit"),
        Pattern.compile("(?i)due\\s+for\\s+debit"),
        Pattern.compile("(?i)has\\s+due\\s+of"),
        Pattern.compile("(?i)pay\\s+at\\s+[a-z0-9._/-]+"),
        Pattern.compile("(?i)to\\s+view/download\\s+statement"),
        Pattern.compile("(?i)view\\s+soa\\s+for\\s+recent"),
        Pattern.compile("(?i)fingerprint\\s+login"),
        Pattern.compile("(?i)biometric\\s+authentication"),
        Pattern.compile("(?i)card\\s+usage\\s+settings"),
        Pattern.compile("(?i)deregistered\\s+from"),
        Pattern.compile("(?i)modification\\s+success"),
        Pattern.compile("(?i)deleted\\s+standing\\s+instruction"),
        Pattern.compile("(?i)customer\\s+care"),
        Pattern.compile("(?i)धोखाधड़ी\\s+से\\s+खुद\\s+को\\s+सुरक्षित"),
        Pattern.compile("(?i)has\\s+been\\s+initiated"),
        Pattern.compile("(?i)refund.*initiated"),
        Pattern.compile("(?i)available\\s+bal.*is\\s+inr"),
        Pattern.compile("(?i)available\\s+bal.*is\\s+rs"),
        Pattern.compile("(?i)for\\s+updated\\s+a/c\\s+bal"),
        Pattern.compile("(?i)cheques\\s+are\\s+subject\\s+to\\s+clearing"),
        Pattern.compile("(?i)maintain\\s+balance"),
        Pattern.compile("(?i)due\\s+on"),
        Pattern.compile("(?i)reminder\\s+to\\s+pay"),
        Pattern.compile("(?i)reward\\s+points"),
        Pattern.compile("(?i)due\\s+in\\s+\\d+\\s+days"),
        Pattern.compile("(?i)is\\s+due"),
        Pattern.compile("(?i)due\\s+date"),
        Pattern.compile("(?i)bill\\s+due"),
        Pattern.compile("(?i)pay\\s+instantly"),
        Pattern.compile("(?i)avoid\\s+late"),
        Pattern.compile("(?i)upcoming\\s+(?:bill|payment)"),
        Pattern.compile("(?i)due\\s+by"),
        Pattern.compile("(?i)pay\\s+before"),
        Pattern.compile("(?i)overdue"),
        Pattern.compile("(?i)pay\\s+now\\s+to"),
        Pattern.compile("(?i)to\\s+avoid\\s+late"),
        Pattern.compile("(?i)bill\\s+(?:is\\s+)?generated")
    )

    private val NEWS_DIGEST_PATTERNS = listOf(
        Pattern.compile("(?i)\\bdigest\\b"),
        Pattern.compile("(?i)\\bnewsletter\\b"),
        Pattern.compile("(?i)\\bmarket update\\b"),
        Pattern.compile("(?i)\\bcapital inflow\\b"),
        Pattern.compile("(?i)\\bforeign capital\\b"),
        Pattern.compile("(?i)\\border & more\\b"),
        Pattern.compile("(?i)\\bcr order\\b"),
        Pattern.compile("(?i)\\bcrore order\\b"),
        Pattern.compile("(?i)\\bbillion\\b"),
        Pattern.compile("(?i)\\btrillion\\b"),
        Pattern.compile("(?i)\\bsensex\\b"),
        Pattern.compile("(?i)\\bnifty\\b"),
        Pattern.compile("(?i)\\bstock alert\\b"),
        Pattern.compile("(?i)\\bmarket wrap\\b"),
        Pattern.compile("(?i)\\bheadline\\b"),
        Pattern.compile("(?i)\\bnews update\\b")
    )

    // Debit action triggers - mandatory currency symbol or explicit debit phrase
    private val DEBIT_PATTERNS = listOf(
        Pattern.compile("(?i)debited\\s+by\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)debited\\s+for\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)debited\\s+with\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:has\\s+been|was|is)?\\s*debited"),
        Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:is|was)?\\s*spent"),
        Pattern.compile("(?i)spent\\s+using\\s+.*?\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)spent\\s+at\\s+.*?\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)spent\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)spent\\s+(?:inr|rs\\.?|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)paid\\s+(?:inr|rs\\.?|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)sent\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)withdrawn\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)deducted\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)txn\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)dr\\s+of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)(?:trxn|txn|transaction)\\.?\\s+(?:of\\s+)?(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)purchase\\s+of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)payment\\s+of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+using.*successful")
    )

    // Credit action triggers
    private val CREDIT_PATTERNS = listOf(
        Pattern.compile("(?i)cashback\\s+of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+cashback\\s+received"),
        Pattern.compile("(?i)refund\\s+of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+has\\s+been\\s+processed"),
        Pattern.compile("(?i)refund\\s+from\\s+.*?amount:\\s*(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)has\\s+credit\\s+for\\s+.*?of\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)credited\\s+by\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)credited\\s+with\\s+(?:rs\\.?|inr|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)credited\\s+to\\s+.*?\\s*(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Pattern.compile("(?i)(?:inr|rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:has\\s+been|was|is)?\\s*credited"),
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
        for (pattern in FAILED_TRANSACTION_PATTERNS) {
            if (pattern.matcher(text).find()) return null
        }
        for (pattern in PROMOTIONAL_OFFER_PATTERNS) {
            if (pattern.matcher(text).find()) return null
        }
        for (pattern in NEWS_DIGEST_PATTERNS) {
            if (pattern.matcher(text).find()) return null
        }
        val cleanText = text.replace("\n", " ").trim()
        for (pattern in INFORMATIONAL_STATEMENT_PATTERNS) {
            if (pattern.matcher(cleanText).find()) return null
        }

        // 2. STAGE 2: TEXT SANITIZATION & PRE-PROCESSING
        val textWithoutBalance = stripBalanceAndLimitClauses(cleanText)

        // 3. STAGE 3: SEMANTIC ACTION CLASSIFICATION

        // 3.1 Check Credit Card Bill Payment confirmation (MUST run before generic payment receipts!)
        if (isCreditCardBillPayment(cleanText)) {
            val amount = extractGenericAmount(textWithoutBalance)
            if (amount > 0.0) {
                val lower = cleanText.lowercase()
                val merchant = if (lower.contains("cred club") || lower.contains("to cred")) "CRED Club"
                               else if (lower.contains("cheq")) "CheQ"
                               else "Credit Card Bill"
                val accountDigits = extractAccountDigits(cleanText)
                return ParsedTransaction(
                    amount = amount,
                    type = "TRANSFER",
                    merchant = merchant,
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

        // 3.3 Check Investment Transactions (SIP, Mutual Funds, Stocks, Zerodha, Groww, INDmoney, NPS, PPF, FD)
        if (isInvestmentTransaction(cleanText)) {
            val amount = extractGenericAmount(textWithoutBalance)
            if (amount > 0.0) {
                val merchant = extractMerchant(cleanText, "INVESTMENT")
                val accountDigits = extractAccountDigits(cleanText)
                return ParsedTransaction(
                    amount = amount,
                    type = "INVESTMENT",
                    merchant = if (merchant.isNotBlank() && merchant != "Unknown Merchant") merchant else "Investment",
                    accountDigits = accountDigits,
                    category = "Investments",
                    rawText = text
                )
            }
        }

        // 3.4 Check Outbound NEFT Transfer (e.g. NEFT transfer credited to <Beneficiary Name>)
        if (isOutboundNeftTransfer(cleanText)) {
            val amount = extractGenericAmount(textWithoutBalance)
            if (amount > 0.0) {
                val merchant = extractMerchant(cleanText, "DEBIT")
                val accountDigits = extractAccountDigits(cleanText)
                val category = predictCategory(merchant, cleanText, "DEBIT")
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

        // 3.5 Check Merchant/Service Payment Receipts (e.g., Insurance, MoRTH Fee, Airtel Bills, ACT Fibernet)
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

        // 3.6 Check CREDIT patterns (Genuine External Income, Cashbacks, Processed Refunds)
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

        // 3.7 Check DEBIT patterns
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

        return null
    }

    private fun isOutboundNeftTransfer(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.contains("neft") || lower.contains("money transfer")) &&
               (lower.contains("credited to") || lower.contains("has been credited to")) &&
               !lower.contains("credited to your a/c") &&
               !lower.contains("credited to your account") &&
               !lower.contains("credited to your card")
    }

    private fun isPaymentReceiptForExpense(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.contains("received payment") || lower.contains("received a payment") || lower.contains("received rs") || lower.contains("received inr") || lower.contains("payment received") || lower.contains("payment of") || lower.contains("receipt of") || lower.contains("confirmation for receipt")) &&
               (lower.contains("for your") || lower.contains("against") || lower.contains("fee") || lower.contains("insurance") || lower.contains("morth") || lower.contains("policy") || lower.contains("acko") || lower.contains("receipt no") || lower.contains("airtel") || lower.contains("bill") || lower.contains("act fibernet"))
    }

    private fun isCreditCardBillPayment(text: String): Boolean {
        val lower = text.lowercase()
        // Reject bill due reminders, upcoming payment notices, or cashback credits
        if (lower.contains("due in") || lower.contains("is due") || lower.contains("due date") || lower.contains("pay instantly") || lower.contains("reminder") || lower.contains("cashback")) return false

        // Check explicit UPI VPA / CC Bill payment services (CRED, CheQ, Billdesk, BBPS)
        if (CC_UPI_VPA_PATTERN.matcher(text).find()) return true
        if (lower.contains("cred.club") || lower.contains("cred app") || lower.contains(" to cred") || lower.contains("cheq") || lower.contains("billdesk") || lower.contains("bbps")) return true

        // Reject merchant card spending (e.g. Paid inr 250 with your BOBCARD to raz*uniorbit)
        if (lower.contains("with your") || lower.contains("spent on") || lower.contains("spent using") || lower.contains(" to raz") || lower.contains(" to merchant")) return false

        val hasCardKeyword = lower.contains("credit card") || lower.contains("bobcard") || lower.contains("card xx") || lower.contains("card ending") || lower.contains("card no") || lower.contains("card ending in") || lower.contains("cc payment") || lower.contains("card bill")
        val hasBillPaymentConfirmation = lower.contains("payment received") || lower.contains("credited towards") || lower.contains("credited with") || lower.contains("received towards") || lower.contains("towards credit card") || lower.contains("towards card") || lower.contains("towards bill payment") || lower.contains("received on your")
        return hasCardKeyword && hasBillPaymentConfirmation
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
        val lower = text.lowercase()
        // Reject news/market magnitude scales like $32 billion, 5000 cr, 30 days
        if (lower.contains("billion") || lower.contains("million") || lower.contains("trillion") || lower.contains("crore") || lower.contains("cr order")) return 0.0

        val pattern = Pattern.compile("(?i)(?:rs\\.?|inr|₹|amt|amount|usd|\\$)\\s*:?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?!billion|million|trillion|crore|cr|lakh|points|%|percent|days|hrs|hours|min|mins)")
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }
        val patternSuffix = Pattern.compile("(?i)([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:rs\\.?|inr|₹|usd|\\$)\\s*(?!billion|million|trillion|crore|cr|lakh|points|%|percent|days|hrs|hours|min|mins)")
        val matcherSuffix = patternSuffix.matcher(text)
        if (matcherSuffix.find()) {
            return matcherSuffix.group(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }
        return 0.0
    }

    private fun extractMerchant(text: String, type: String): String {
        val lowerFull = text.lowercase()
        if (lowerFull.contains("airtel")) {
            if (lowerFull.contains("black")) return "Airtel Black"
            if (lowerFull.contains("wi-fi") || lowerFull.contains("wifi")) return "Airtel Wi-Fi"
            return "Airtel"
        }
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
        val atMerchant = Pattern.compile("(?i)\\s+at\\s+([A-Za-z0-9._\\-@#\\s]+?)(?:,?\\s+is\\s+eligible|,?\\s+on\\s+[0-9]{2}-[0-9]{2}|\\s+by\\s+upi|\\.\\s*available|\\.\\s*avl|\\.|$)").matcher(cleanText)
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
        val toPayee = Pattern.compile("(?i)\\s+to\\s+([A-Za-z0-9\\s@._\\-*]+?)(?:\\s+on\\s+|\\s+via\\s+|\\s+at\\s+|\\s+ref|\\.|$)").matcher(cleanText)
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
        if (lowerRaw.contains("amazon pay wall")) return "AMAZON PAY WALL"
        if (lowerRaw.contains("amazon")) return "Amazon"
        if (lowerRaw.contains("acko")) return "ACKO Insurance"
        if (lowerRaw.contains("morth")) return "MoRTH"

        if (clean.contains("@")) {
            var handlePrefix = clean.split("@")[0].trim()
            handlePrefix = handlePrefix
                .replace(Regex("(?i)^upi-m\\s*|-upi$|^upi-|paytm$|ybl$|icici$"), "")
                .trim()
            if (handlePrefix.isNotBlank()) {
                clean = handlePrefix
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

    private fun isInvestmentTransaction(text: String): Boolean {
        val lower = text.lowercase()
        val investmentKeywords = listOf(
            "\\bsip\\b", "\\bmutual funds?\\b", "\\bzerodha\\b", "\\bgroww\\b", "\\bindmoney\\b",
            "\\bupstox\\b", "\\bangel one\\b", "\\bsmallcase\\b", "\\bnps\\b", "\\bppf\\b",
            "\\bfixed deposit\\b", "\\bfd\\b", "\\blic\\b", "\\bmotilal oswal\\b",
            "\\bcoinswitch\\b", "\\bwazirx\\b", "\\bsharekhan\\b"
        )
        val matchesBrand = investmentKeywords.any { Regex(it).containsMatchIn(lower) }
        if (!matchesBrand) return false

        val executionKeywords = listOf(
            "debited", "spent", "invested", "purchased", "bought", "units allocated",
            "order executed", "autopay executed", "deducted", "credited", "payment of", "paid", "sent", "mandate", "upi mandate"
        )
        val hasExecution = executionKeywords.any { lower.contains(it) }

        // Reject news/market article terms
        val hasNews = lower.contains("billion") || lower.contains("trillion") || lower.contains("digest") ||
                lower.contains("inflow") || lower.contains("sensex") || lower.contains("nifty") ||
                lower.contains("cr order") || lower.contains("crore order")

        return hasExecution && !hasNews
    }

    private fun predictCategory(merchant: String, rawText: String, type: String): String {
        if (type == "CREDIT") return "Income"
        if (type == "INVESTMENT") return "Investments"

        val lower = "$merchant $rawText".lowercase()
        return when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("cafe") || lower.contains("atithi") || lower.contains("restaurant") || lower.contains("bakery") || lower.contains("dining") || lower.contains("dominos") || lower.contains("mcdonald") || lower.contains("starbucks") || lower.contains("kfc") -> "Food & Dining"
            lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("irctc") || lower.contains("fuel") || lower.contains("petrol") || lower.contains("fastag") || lower.contains("toll") || lower.contains("railway") || lower.contains("metro") -> "Transportation"
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("ajio") || lower.contains("dmart") || lower.contains("blinkit") || lower.contains("zepto") || lower.contains("bigbasket") || lower.contains("mart") || lower.contains("store") || lower.contains("retail") -> "Shopping"
            lower.contains("airtel") || lower.contains("jio") || lower.contains("vi ") || lower.contains("bill") || lower.contains("recharge") || lower.contains("bescom") || lower.contains("electricity") || lower.contains("water") || lower.contains("gas") || lower.contains("broadband") || lower.contains("tata play") || lower.contains("dth") || lower.contains("fibernet") || lower.contains("act ") || lower.contains("actgrp") -> "Bills & Utilities"
            lower.contains("ksec") || lower.contains("zerodha") || lower.contains("groww") || lower.contains("upstox") || lower.contains("trading") || lower.contains("mutual fund") || lower.contains("sip") || lower.contains("coin") || lower.contains("stock") -> "Investments"
            lower.contains("netflix") || lower.contains("prime video") || lower.contains("spotify") || lower.contains("youtube") || lower.contains("bookmyshow") || lower.contains("pvr") || lower.contains("inox") || lower.contains("hotstar") || lower.contains("cinema") -> "Entertainment"
            lower.contains("pharmacy") || lower.contains("apollo") || lower.contains("practo") || lower.contains("hospital") || lower.contains("diagnostic") || lower.contains("medical") || lower.contains("pharmeasy") || lower.contains("1mg") || lower.contains("clinic") -> "Health & Medical"
            lower.contains("insurance") || lower.contains("acko") || lower.contains("policy") || lower.contains("lic") -> "Insurance"
            lower.contains("morth") || lower.contains("rto") || lower.contains("fee") || lower.contains("registration fee") || lower.contains("tax") -> "Government & Fees"
            else -> "General Expense"
        }
    }
}
