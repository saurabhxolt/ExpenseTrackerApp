package com.expensetracker.app.core.analysis

import com.expensetracker.app.core.database.entity.CreditCardEntity
import com.expensetracker.app.core.database.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CreditCardRadarCalculatorTest {

    @Test
    fun testBillingCycleBoundsBeforeStatementDay() {
        // Assume reference date is October 10, 2026, statement day is 15th
        val refCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val refMs = refCalendar.timeInMillis

        val (cycleStartMs, cycleEndMs) = CreditCardRadarCalculator.computeBillingCycleBounds(15, refMs)

        val startCal = Calendar.getInstance().apply { timeInMillis = cycleStartMs }
        assertEquals(Calendar.SEPTEMBER, startCal.get(Calendar.MONTH))
        assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        val endCal = Calendar.getInstance().apply { timeInMillis = cycleEndMs }
        assertEquals(Calendar.OCTOBER, endCal.get(Calendar.MONTH))
        assertEquals(14, endCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testBillingCycleBoundsAfterStatementDay() {
        // Assume reference date is October 25, 2026, statement day is 15th
        val refCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 25, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val refMs = refCalendar.timeInMillis

        val (cycleStartMs, cycleEndMs) = CreditCardRadarCalculator.computeBillingCycleBounds(15, refMs)

        val startCal = Calendar.getInstance().apply { timeInMillis = cycleStartMs }
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH))

        val endCal = Calendar.getInstance().apply { timeInMillis = cycleEndMs }
        assertEquals(Calendar.NOVEMBER, endCal.get(Calendar.MONTH))
        assertEquals(14, endCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testPaymentDueDateAndGraceDays() {
        // Reference date: Oct 20, 2026. Statement Day: 15. Due Day: 5 of following month (Nov 5)
        val refCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 20, 10, 0, 0)
        }
        val refMs = refCalendar.timeInMillis

        val dueMs = CreditCardRadarCalculator.computePaymentDueDate(15, 5, refMs)
        val dueCal = Calendar.getInstance().apply { timeInMillis = dueMs }

        assertEquals(Calendar.NOVEMBER, dueCal.get(Calendar.MONTH))
        assertEquals(5, dueCal.get(Calendar.DAY_OF_MONTH))

        val graceDays = CreditCardRadarCalculator.calculateGraceDays(dueMs, refMs)
        assertTrue(graceDays in 16..17) // ~16-17 days
    }

    @Test
    fun testCardUtilizationOptimalBelow30Percent() {
        val card = CreditCardEntity(
            id = 1L,
            cardName = "HDFC Regalia",
            last4Digits = "4321",
            totalLimit = 100000.0,
            statementDayOfMonth = 15,
            dueDayOfMonth = 5
        )

        val refMs = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 20, 12, 0) }.timeInMillis
        val tx1 = TransactionEntity(
            id = 1L,
            amount = 25000.0,
            type = "DEBIT",
            merchant = "Amazon",
            rawText = "Spent on card ending 4321",
            category = "Shopping",
            timestamp = refMs - 10000L,
            accountDigits = "XX4321",
            transactionHash = "hash1"
        )

        val status = CreditCardRadarCalculator.analyzeCard(card, listOf(tx1), refMs)

        assertEquals(25000.0, status.currentOutstanding, 0.01)
        assertEquals(25.0, status.utilizationPercent, 0.01)
        assertEquals(UtilizationTier.OPTIMAL, status.utilizationTier)
        assertEquals(30000.0, status.safe30PercentLimit, 0.01)
        assertEquals(5000.0, status.safe30RemainingHeadroom, 0.01)
        assertEquals(75000.0, status.availableCredit, 0.01)
        assertFalse(status.isOverdue)
    }

    @Test
    fun testCardUtilizationCautionAndCriticalTiers() {
        val card = CreditCardEntity(
            id = 2L,
            cardName = "ICICI Amazon Pay",
            last4Digits = "9876",
            totalLimit = 50000.0,
            statementDayOfMonth = 1,
            dueDayOfMonth = 20
        )

        val refMs = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 10, 12, 0) }.timeInMillis

        // Spend 20,000 / 50,000 = 40% (Caution)
        val txCaution = TransactionEntity(
            id = 2L,
            amount = 20000.0,
            type = "DEBIT",
            merchant = "Croma",
            rawText = "Paid on ICICI Credit Card",
            category = "Electronics",
            timestamp = refMs - 10000L,
            accountDigits = "9876",
            transactionHash = "hash2"
        )
        val statusCaution = CreditCardRadarCalculator.analyzeCard(card, listOf(txCaution), refMs)
        assertEquals(40.0, statusCaution.utilizationPercent, 0.01)
        assertEquals(UtilizationTier.CAUTION, statusCaution.utilizationTier)
        assertEquals(0.0, statusCaution.safe30RemainingHeadroom, 0.01) // 30% is 15k, spent 20k

        // Additional spend of 15,000 -> Total 35,000 / 50,000 = 70% (Critical)
        val txCritical = TransactionEntity(
            id = 3L,
            amount = 15000.0,
            type = "DEBIT",
            merchant = "MakeMyTrip",
            rawText = "Card ending 9876",
            category = "Travel",
            timestamp = refMs - 5000L,
            accountDigits = "9876",
            transactionHash = "hash3"
        )
        val statusCritical = CreditCardRadarCalculator.analyzeCard(card, listOf(txCaution, txCritical), refMs)
        assertEquals(70.0, statusCritical.utilizationPercent, 0.01)
        assertEquals(UtilizationTier.CRITICAL, statusCritical.utilizationTier)

        // Now pay bill with TRANSFER of 25,000 -> Outstanding drops to 10,000 / 50,000 = 20% (Optimal)
        val txRepayment = TransactionEntity(
            id = 4L,
            amount = 25000.0,
            type = "TRANSFER",
            merchant = "CRED Card Payment",
            rawText = "Credit card payment for ICICI Card 9876",
            category = "Bills & Utilities",
            timestamp = refMs - 1000L,
            accountDigits = "9876",
            transactionHash = "hash4"
        )
        val statusRecovered = CreditCardRadarCalculator.analyzeCard(card, listOf(txCaution, txCritical, txRepayment), refMs)
        assertEquals(10000.0, statusRecovered.currentOutstanding, 0.01)
        assertEquals(20.0, statusRecovered.utilizationPercent, 0.01)
        assertEquals(UtilizationTier.OPTIMAL, statusRecovered.utilizationTier)
    }

    @Test
    fun testDiscoverUnregisteredCards() {
        val registered = listOf(
            CreditCardEntity(id = 1L, cardName = "Registered HDFC", last4Digits = "1111", totalLimit = 50000.0)
        )

        val txs = listOf(
            TransactionEntity(
                id = 1L,
                amount = 1200.0,
                type = "DEBIT",
                merchant = "Swiggy",
                rawText = "Rs 1200.00 spent on your SBI Credit Card ending with 4444 on 12-Oct-26",
                category = "Food & Dining",
                timestamp = System.currentTimeMillis(),
                accountDigits = "4444",
                transactionHash = "h1"
            ),
            TransactionEntity(
                id = 2L,
                amount = 4500.0,
                type = "DEBIT",
                merchant = "Zara",
                rawText = "Alert: Rs 4500 spent on HDFC Credit Card ending 1111", // Already registered, should ignore
                category = "Shopping",
                timestamp = System.currentTimeMillis(),
                accountDigits = "1111",
                transactionHash = "h2"
            )
        )

        val discovered = CreditCardRadarCalculator.discoverUnregisteredCards(txs, registered)
        assertEquals(1, discovered.size)
        assertEquals("4444", discovered[0].last4Digits)
        assertEquals("SBI", discovered[0].suggestedBank)
        assertEquals(1200.0, discovered[0].totalSpendRecorded, 0.01)
    }

    @Test
    fun testIsTransactionForCardWithNotesAndMaskedDigits() {
        val card = CreditCardEntity(
            id = 1L,
            cardName = "HDFC Regalia Gold",
            last4Digits = "4321",
            totalLimit = 200000.0,
            bankName = "HDFC"
        )

        // 1. Transaction tagged in note
        val txWithNote = TransactionEntity(
            id = 10L,
            amount = 1500.0,
            type = "DEBIT",
            merchant = "Fine Dine",
            rawText = "Manual expense",
            category = "Food & Dining",
            timestamp = System.currentTimeMillis(),
            note = "Paid with HDFC Regalia 4321",
            accountDigits = "",
            transactionHash = "hash10"
        )
        assertTrue(CreditCardRadarCalculator.isTransactionForCard(txWithNote, card))

        // 2. Transaction with masked account digits
        val txWithMask = TransactionEntity(
            id = 11L,
            amount = 3000.0,
            type = "DEBIT",
            merchant = "Blinkit",
            rawText = "Paid via CC **4321",
            category = "Groceries",
            timestamp = System.currentTimeMillis(),
            note = "",
            accountDigits = "**4321",
            transactionHash = "hash11"
        )
        assertTrue(CreditCardRadarCalculator.isTransactionForCard(txWithMask, card))

        // 3. Unrelated transaction
        val txUnrelated = TransactionEntity(
            id = 12L,
            amount = 500.0,
            type = "DEBIT",
            merchant = "Chai Point",
            rawText = "UPI payment to Chai Point",
            category = "Food & Dining",
            timestamp = System.currentTimeMillis(),
            note = "Paid via GPay UPI",
            accountDigits = "9876",
            transactionHash = "hash12"
        )
        assertFalse(CreditCardRadarCalculator.isTransactionForCard(txUnrelated, card))
    }
}
