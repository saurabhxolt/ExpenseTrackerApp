package com.expensetracker.app.core.analysis

import com.expensetracker.app.core.database.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxDeductionAnalyzerTest {

    private fun testTransaction(
        amount: Double,
        type: String = "DEBIT",
        merchant: String = "Test Merchant",
        category: String = "General Expense",
        rawText: String = "",
        timestamp: Long = System.currentTimeMillis(),
        taxDeductibleSection: String? = null
    ): TransactionEntity {
        return TransactionEntity(
            id = 1L,
            amount = amount,
            type = type,
            merchant = merchant,
            rawText = rawText,
            category = category,
            timestamp = timestamp,
            transactionHash = "hash-${System.nanoTime()}",
            taxDeductibleSection = taxDeductibleSection
        )
    }

    @Test
    fun testInferSection80C() {
        assertEquals("80C", TaxDeductionAnalyzer.inferSection("LIC of India", "Investment", "Premium paid"))
        assertEquals("80C", TaxDeductionAnalyzer.inferSection("SBI Mutual Fund", "Investment", "ELSS Tax Saver scheme"))
        assertEquals("80C", TaxDeductionAnalyzer.inferSection("Public Provident Fund", "Investment", "PPF deposit"))
        assertEquals("80C", TaxDeductionAnalyzer.inferSection("Delhi Public School", "Education", "Tuition fee payment"))
    }

    @Test
    fun testInferSection80D() {
        assertEquals("80D", TaxDeductionAnalyzer.inferSection("Star Health Insurance", "Insurance", "Family mediclaim policy"))
        assertEquals("80D", TaxDeductionAnalyzer.inferSection("Niva Bupa Health", "Insurance", "Health insurance renewal"))
        assertEquals("80D", TaxDeductionAnalyzer.inferSection("Care Health Insurance", "Bills & Utilities", "Annual premium"))
    }

    @Test
    fun testInferSection10_13A_HRA() {
        assertEquals("10(13A)", TaxDeductionAnalyzer.inferSection("Mr. Sharma", "Rent", "Paid house rent for October"))
        assertEquals("10(13A)", TaxDeductionAnalyzer.inferSection("Landlord Rent", "Housing", "Flat rent transfer"))
    }

    @Test
    fun testInferSectionOtherSections() {
        assertEquals("80CCD(1B)", TaxDeductionAnalyzer.inferSection("CRA NSDL", "Investment", "Tier 1 NPS contribution"))
        assertEquals("80E", TaxDeductionAnalyzer.inferSection("SBI Education Loan", "Education", "Education loan EMI interest"))
        assertEquals("80G", TaxDeductionAnalyzer.inferSection("PM CARES Fund", "Donation", "Relief donation"))
    }

    @Test
    fun testNonTaxTransactionReturnsNull() {
        assertNull(TaxDeductionAnalyzer.inferSection("Swiggy", "Food & Dining", "Order delivered"))
        assertNull(TaxDeductionAnalyzer.inferSection("Uber India", "Transportation", "Ride fare"))
    }

    @Test
    fun testComputeSummaryEnforcesSectionCaps() {
        val now = System.currentTimeMillis()
        val txs = listOf(
            // Section 80C: Total ₹1,80,000 (exceeds ₹1,50,000 statutory cap)
            testTransaction(amount = 100000.0, merchant = "LIC", category = "Insurance", rawText = "Life insurance premium", timestamp = now),
            testTransaction(amount = 80000.0, merchant = "PPF", category = "Investment", rawText = "Public provident fund", timestamp = now),

            // Section 80D: Total ₹30,000 (exceeds ₹25,000 statutory cap)
            testTransaction(amount = 30000.0, merchant = "Star Health Insurance", category = "Insurance", rawText = "Family health plan", timestamp = now),

            // Section 10(13A): Total ₹25,000 (Rent has no cap)
            testTransaction(amount = 25000.0, merchant = "Landlord", category = "Rent", rawText = "House rent", timestamp = now)
        )

        val summary = TaxDeductionAnalyzer.computeSummary(txs, forCurrentFyOnly = false)

        // Effective Claimed = 150,000 (80C capped) + 25,000 (80D capped) + 25,000 (HRA) = 200,000
        assertEquals(200000.0, summary.totalClaimedAmount, 0.001)

        // Estimated savings at 20% slab = 40,000
        assertEquals(40000.0, summary.estimatedTaxSavings, 0.001)

        // Raw totals before capping
        assertEquals(180000.0, summary.sectionTotals["80C"] ?: 0.0, 0.001)
        assertEquals(30000.0, summary.sectionTotals["80D"] ?: 0.0, 0.001)
        assertEquals(25000.0, summary.sectionTotals["10(13A)"] ?: 0.0, 0.001)
    }

    @Test
    fun testFinancialYearLabel() {
        val label = TaxDeductionAnalyzer.getFinancialYearLabel()
        assertTrue(label.startsWith("FY 20"))
    }
}
