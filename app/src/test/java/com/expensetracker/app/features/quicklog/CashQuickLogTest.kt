package com.expensetracker.app.features.quicklog

import com.expensetracker.app.core.database.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CashQuickLogTest {

    @Test
    fun testCashPresetsValues() {
        val presets = listOf(
            CashPreset("☕", "Chai / Tea", 20.0, "Food & Dining"),
            CashPreset("🛺", "Auto / Rickshaw", 50.0, "Transportation"),
            CashPreset("🥪", "Snacks / Breakfast", 100.0, "Food & Dining"),
            CashPreset("🍛", "Lunch / Thali", 150.0, "Food & Dining"),
            CashPreset("🛒", "Groceries / Kirana", 500.0, "Groceries")
        )

        assertEquals(5, presets.size)
        assertEquals(20.0, presets[0].amount, 0.01)
        assertEquals("☕", presets[0].emoji)
        assertEquals(50.0, presets[1].amount, 0.01)
        assertEquals("Transportation", presets[1].category)
    }

    @Test
    fun testCreateCashTransactionEntity() {
        val amount = 50.0
        val category = "Transportation"
        val merchant = "Cash Expense"
        val note = "Auto to Metro"
        val now = System.currentTimeMillis()

        val tx = TransactionEntity(
            amount = amount,
            type = "DEBIT",
            merchant = merchant,
            rawText = "Cash spend: $note",
            category = category,
            timestamp = now,
            note = note,
            isManual = true,
            transactionHash = "cash_${now}_1234"
        )

        assertEquals(50.0, tx.amount, 0.01)
        assertEquals("DEBIT", tx.type)
        assertEquals("Cash Expense", tx.merchant)
        assertEquals("Transportation", tx.category)
        assertTrue(tx.isManual)
        assertEquals("Auto to Metro", tx.note)
        assertTrue(tx.transactionHash.startsWith("cash_"))
    }
}
