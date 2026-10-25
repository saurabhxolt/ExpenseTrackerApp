package com.expensetracker.app.core.analysis

import com.expensetracker.app.core.database.entity.CreditCardEntity
import com.expensetracker.app.core.database.entity.TransactionEntity
import java.util.Calendar

enum class UtilizationTier {
    OPTIMAL,  // < 30.0% (Safe • CIBIL score protected)
    CAUTION,  // 30.0% .. 50.0% (Approaching warning)
    CRITICAL  // > 50.0% (High utilization penalty risk)
}

data class CreditCardRadarStatus(
    val card: CreditCardEntity,
    val currentSpend: Double,
    val totalTransfersPaid: Double,
    val currentOutstanding: Double,
    val availableCredit: Double,
    val utilizationPercent: Double,
    val utilizationTier: UtilizationTier,
    val safe30PercentLimit: Double,
    val safe30RemainingHeadroom: Double,
    val cycleStartDateMs: Long,
    val cycleEndDateMs: Long,
    val paymentDueDateMs: Long,
    val graceDaysRemaining: Int,
    val isOverdue: Boolean,
    val matchingTransactions: List<TransactionEntity>
)

data class DiscoveredCreditCard(
    val last4Digits: String,
    val suggestedBank: String,
    val suggestedName: String,
    val detectedDebitsCount: Int,
    val totalSpendRecorded: Double
)

object CreditCardRadarCalculator {

    const val SAFE_UTILIZATION_THRESHOLD = 30.0
    const val CAUTION_UTILIZATION_THRESHOLD = 50.0

    /**
     * Computes the start and end timestamp of the current billing cycle.
     */
    fun computeBillingCycleBounds(statementDay: Int, referenceTimeMs: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply { timeInMillis = referenceTimeMs }
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val cycleStart = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (currentDay >= statementDay) {
            val maxDay = cycleStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            cycleStart.set(Calendar.DAY_OF_MONTH, statementDay.coerceAtMost(maxDay))
        } else {
            cycleStart.add(Calendar.MONTH, -1)
            val maxDay = cycleStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            cycleStart.set(Calendar.DAY_OF_MONTH, statementDay.coerceAtMost(maxDay))
        }

        val cycleEnd = (cycleStart.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }

        return Pair(cycleStart.timeInMillis, cycleEnd.timeInMillis)
    }

    /**
     * Computes the upcoming payment due date for the active billing cycle.
     */
    fun computePaymentDueDate(
        statementDay: Int,
        dueDay: Int,
        referenceTimeMs: Long = System.currentTimeMillis()
    ): Long {
        val (cycleStartMs, _) = computeBillingCycleBounds(statementDay, referenceTimeMs)
        val dueDate = Calendar.getInstance().apply {
            timeInMillis = cycleStartMs
            if (dueDay <= statementDay) {
                add(Calendar.MONTH, 1)
            }
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, dueDay.coerceAtMost(maxDay))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return dueDate.timeInMillis
    }

    /**
     * Calculates days remaining until payment due date.
     */
    fun calculateGraceDays(paymentDueDateMs: Long, referenceTimeMs: Long = System.currentTimeMillis()): Int {
        val diffMs = paymentDueDateMs - referenceTimeMs
        return kotlin.math.ceil(diffMs.toDouble() / (24 * 3600 * 1000.0)).toInt()
    }

    /**
     * Checks if a transaction belongs to a given credit card.
     */
    fun isTransactionForCard(tx: TransactionEntity, card: CreditCardEntity): Boolean {
        val last4 = card.last4Digits.filter { it.isDigit() }
        val accountClean = tx.accountDigits.filter { it.isDigit() }

        // 1. Direct last 4 digits matching on accountDigits
        if (last4.isNotEmpty()) {
            if (accountClean.endsWith(last4) || accountClean.contains(last4)) return true
            if (tx.accountDigits.contains(last4, ignoreCase = true)) return true
        }

        // 2. Note inspection (manual transactions, tagged receipts, or custom notes)
        val note = tx.note.lowercase()
        if (last4.isNotEmpty() && note.contains(last4)) return true
        val cardName = card.cardName.trim().lowercase()
        if (cardName.length >= 3 && note.contains(cardName)) return true

        // 3. Raw SMS text inspection
        val raw = tx.rawText.lowercase()
        if (last4.isNotEmpty()) {
            if (raw.contains(last4)) return true
            // Common masked representations: xx4321, **4321, x4321, *4321
            if (raw.contains("xx$last4") || raw.contains("**$last4") || raw.contains("x$last4") || raw.contains("*$last4")) return true
        }
        if (cardName.length >= 3 && raw.contains(cardName)) return true

        // 4. Merchant text inspection
        val merchant = tx.merchant.lowercase()
        if (last4.isNotEmpty() && merchant.contains(last4)) return true
        if (cardName.length >= 3 && merchant.contains(cardName)) return true

        // 5. Bank name indicator check
        val bank = card.bankName.trim().lowercase()
        if (bank.isNotEmpty() && bank != "bank" && (raw.contains(bank) || merchant.contains(bank) || note.contains(bank))) {
            val hasCardIndicator = raw.contains("credit card") || raw.contains("card") ||
                    raw.contains("cc") || merchant.contains("card") || note.contains("card")
            if (hasCardIndicator) return true
        }

        return false
    }

    /**
     * Analyzes card utilization, outstanding balance, safe headroom, and grace period countdown.
     */
    fun analyzeCard(
        card: CreditCardEntity,
        transactions: List<TransactionEntity>,
        referenceTimeMs: Long = System.currentTimeMillis()
    ): CreditCardRadarStatus {
        val (cycleStartMs, cycleEndMs) = computeBillingCycleBounds(card.statementDayOfMonth, referenceTimeMs)
        val paymentDueDateMs = computePaymentDueDate(card.statementDayOfMonth, card.dueDayOfMonth, referenceTimeMs)
        val graceDays = calculateGraceDays(paymentDueDateMs, referenceTimeMs)
        val isOverdue = graceDays < 0

        // Filter transactions within the active billing cycle
        val cycleTxs = transactions.filter { tx ->
            tx.timestamp in cycleStartMs..referenceTimeMs && isTransactionForCard(tx, card)
        }

        var debits = 0.0
        var transfers = 0.0

        for (tx in cycleTxs) {
            when (tx.type.uppercase()) {
                "DEBIT" -> debits += tx.amount
                "TRANSFER" -> transfers += tx.amount
                "CREDIT" -> {
                    // Cashback or refund
                    transfers += tx.amount
                }
            }
        }

        val currentOutstanding = (debits - transfers).coerceAtLeast(0.0)
        val availableCredit = (card.totalLimit - currentOutstanding).coerceAtLeast(0.0)
        val utilizationPercent = if (card.totalLimit > 0) {
            (currentOutstanding / card.totalLimit) * 100.0
        } else 0.0

        val safe30Limit = card.totalLimit * (SAFE_UTILIZATION_THRESHOLD / 100.0)
        val safe30RemainingHeadroom = (safe30Limit - currentOutstanding).coerceAtLeast(0.0)

        val tier = when {
            utilizationPercent < SAFE_UTILIZATION_THRESHOLD -> UtilizationTier.OPTIMAL
            utilizationPercent <= CAUTION_UTILIZATION_THRESHOLD -> UtilizationTier.CAUTION
            else -> UtilizationTier.CRITICAL
        }

        return CreditCardRadarStatus(
            card = card,
            currentSpend = debits,
            totalTransfersPaid = transfers,
            currentOutstanding = currentOutstanding,
            availableCredit = availableCredit,
            utilizationPercent = utilizationPercent,
            utilizationTier = tier,
            safe30PercentLimit = safe30Limit,
            safe30RemainingHeadroom = safe30RemainingHeadroom,
            cycleStartDateMs = cycleStartMs,
            cycleEndDateMs = cycleEndMs,
            paymentDueDateMs = paymentDueDateMs,
            graceDaysRemaining = graceDays,
            isOverdue = isOverdue,
            matchingTransactions = cycleTxs
        )
    }

    /**
     * Auto-discovers unregistered cards from transaction SMS records.
     */
    fun discoverUnregisteredCards(
        transactions: List<TransactionEntity>,
        registeredCards: List<CreditCardEntity>
    ): List<DiscoveredCreditCard> {
        val registeredDigits = registeredCards.map { it.last4Digits.trim() }.filter { it.isNotEmpty() }.toSet()
        val cardRegex = Regex("""(?i)(?:credit\s*card|card\s*ending|card\s*xx+|card\s*no\.?|card\s*ending\s*in)\s*(\d{4})""")
        val standalone4Digits = Regex("""\b(\d{4})\b""")

        val discoveredMap = mutableMapOf<String, Pair<String, Double>>() // last4 -> (bank, totalSpend)
        val countMap = mutableMapOf<String, Int>()

        for (tx in transactions) {
            val raw = tx.rawText
            val lower = raw.lowercase()

            val isCardSms = lower.contains("credit card") || lower.contains("sbi card") ||
                    lower.contains("bobcard") || lower.contains("onecard") ||
                    lower.contains("card ending") || lower.contains("card xx") ||
                    lower.contains("spent on card")

            if (!isCardSms) continue

            var digits = ""
            val match = cardRegex.find(raw)
            if (match != null) {
                digits = match.groupValues[1]
            } else if (tx.accountDigits.isNotEmpty() && tx.accountDigits.length >= 4) {
                digits = tx.accountDigits.takeLast(4)
            } else {
                val digitMatch = standalone4Digits.find(raw)
                if (digitMatch != null) {
                    digits = digitMatch.groupValues[1]
                }
            }

            if (digits.length == 4 && digits !in registeredDigits) {
                val bank = detectBankName(raw, tx.merchant)
                val existing = discoveredMap[digits]
                val currentSpend = (existing?.second ?: 0.0) + (if (tx.type == "DEBIT") tx.amount else 0.0)
                discoveredMap[digits] = Pair(bank, currentSpend)
                countMap[digits] = (countMap[digits] ?: 0) + 1
            }
        }

        return discoveredMap.map { (digits, pair) ->
            val (bank, spend) = pair
            DiscoveredCreditCard(
                last4Digits = digits,
                suggestedBank = bank,
                suggestedName = "$bank Credit Card",
                detectedDebitsCount = countMap[digits] ?: 1,
                totalSpendRecorded = spend
            )
        }.sortedByDescending { it.detectedDebitsCount }
    }

    private fun detectBankName(raw: String, merchant: String): String {
        val lower = "$raw $merchant".lowercase()
        return when {
            lower.contains("hdfc") -> "HDFC"
            lower.contains("sbi") -> "SBI"
            lower.contains("icici") -> "ICICI"
            lower.contains("axis") -> "Axis"
            lower.contains("kotak") -> "Kotak"
            lower.contains("onecard") -> "OneCard"
            lower.contains("bobcard") || lower.contains("bank of baroda") -> "BOB"
            lower.contains("indusind") -> "IndusInd"
            lower.contains("amex") || lower.contains("american express") -> "Amex"
            lower.contains("rbl") -> "RBL"
            lower.contains("yes bank") -> "Yes Bank"
            lower.contains("idfc") -> "IDFC FIRST"
            lower.contains("scapia") || lower.contains("federal") -> "Federal"
            else -> "Bank"
        }
    }
}
