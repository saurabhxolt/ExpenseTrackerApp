package com.expensetracker.app.core.analysis

import com.expensetracker.app.core.analysis.ItemizedSplitCalculator.ItemizedDish
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemizedSplitCalculatorTest {

    @Test
    fun testIndividualAndSharedDishesSplit() {
        val members = listOf("Alice", "Bob", "Charlie")
        val dishes = listOf(
            ItemizedDish(name = "Pasta", price = 300.0, consumers = setOf("Alice")),
            ItemizedDish(name = "Burger", price = 200.0, consumers = setOf("Bob")),
            ItemizedDish(name = "Steak", price = 500.0, consumers = setOf("Charlie")),
            // Shared Garlic Bread by Alice and Bob (100 each)
            ItemizedDish(name = "Garlic Bread", price = 200.0, consumers = setOf("Alice", "Bob"))
        )

        val result = ItemizedSplitCalculator.calculateSplit(
            dishes = dishes,
            allMembers = members,
            taxPercentage = 0.0,
            tipPercentage = 0.0
        )

        // Subtotal = 300 + 200 + 500 + 200 = 1200
        assertEquals(1200.0, result.subtotal, 0.01)

        val alice = result.breakdown.first { it.personName == "Alice" }
        val bob = result.breakdown.first { it.personName == "Bob" }
        val charlie = result.breakdown.first { it.personName == "Charlie" }

        // Alice: 300 + 100 = 400
        assertEquals(400.0, alice.totalAmount, 0.01)
        // Bob: 200 + 100 = 300
        assertEquals(300.0, bob.totalAmount, 0.01)
        // Charlie: 500
        assertEquals(500.0, charlie.totalAmount, 0.01)

        // Invariant: sum of totals must equal subtotal
        val sumTotals = result.breakdown.sumOf { it.totalAmount }
        assertEquals(result.subtotal, sumTotals, 0.05)
    }

    @Test
    fun testProportionalTaxAndTipDistribution() {
        val members = listOf("Alice", "Bob")
        val dishes = listOf(
            ItemizedDish(name = "Expensive Wine", price = 800.0, consumers = setOf("Alice")),
            ItemizedDish(name = "Salad", price = 200.0, consumers = setOf("Bob"))
        )

        // Subtotal = 1000. 10% tax = 100. 10% tip = 100. Grand Total = 1200.
        val result = ItemizedSplitCalculator.calculateSplit(
            dishes = dishes,
            allMembers = members,
            taxPercentage = 10.0,
            tipPercentage = 10.0
        )

        assertEquals(1000.0, result.subtotal, 0.01)
        assertEquals(100.0, result.taxAmount, 0.01)
        assertEquals(100.0, result.tipAmount, 0.01)
        assertEquals(1200.0, result.grandTotal, 0.01)

        val alice = result.breakdown.first { it.personName == "Alice" }
        val bob = result.breakdown.first { it.personName == "Bob" }

        // Alice has 80% of subtotal: 800 base + 80 tax + 80 tip = 960
        assertEquals(800.0, alice.baseAmount, 0.01)
        assertEquals(80.0, alice.taxShare, 0.01)
        assertEquals(80.0, alice.tipShare, 0.01)
        assertEquals(960.0, alice.totalAmount, 0.01)

        // Bob has 20% of subtotal: 200 base + 20 tax + 20 tip = 240
        assertEquals(200.0, bob.baseAmount, 0.01)
        assertEquals(20.0, bob.taxShare, 0.01)
        assertEquals(20.0, bob.tipShare, 0.01)
        assertEquals(240.0, bob.totalAmount, 0.01)

        assertEquals(1200.0, alice.totalAmount + bob.totalAmount, 0.01)
    }

    @Test
    fun testFormatWhatsAppReport() {
        val members = listOf("Alice", "Bob")
        val dishes = listOf(ItemizedDish(name = "Pizza", price = 500.0, consumers = setOf("Alice", "Bob")))
        val result = ItemizedSplitCalculator.calculateSplit(dishes, members, taxPercentage = 5.0)

        val report = ItemizedSplitCalculator.formatWhatsAppReport("Trattoria", result, paidBy = "Alice")
        assertTrue(report.contains("Trattoria"))
        assertTrue(report.contains("Paid by: Alice"))
        assertTrue(report.contains("Alice"))
        assertTrue(report.contains("Bob"))
    }
}
