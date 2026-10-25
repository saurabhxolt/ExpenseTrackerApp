package com.expensetracker.app.core.analysis

import com.expensetracker.app.core.database.entity.RecurringIncomeEntity
import com.expensetracker.app.core.database.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurringInflowReconcilerTest {

    @Test
    fun reconcile_salaryAlert_matchesPrimarySalary() {
        val salaryIncome = RecurringIncomeEntity(
            id = 10,
            name = "Infosys Salary",
            amount = 85000.0,
            expectedDayOfMonth = 1,
            isSalary = true,
            payerPpiOrBank = "INFOSYS"
        )

        val tx = TransactionEntity(
            id = 1,
            amount = 84500.0, // slight variation due to tax/PF
            type = "CREDIT",
            merchant = "INFOSYS LIMITED",
            category = "Income",
            rawText = "Your A/C 1234 is credited with Rs 84,500.00 on 01-OCT-26 by INFOSYS LIMITED SALARY OCT 2026",
            timestamp = System.currentTimeMillis(),
            transactionHash = "hash1"
        )

        val match = RecurringInflowReconciler.reconcile(tx, listOf(salaryIncome))
        assertNotNull(match)
        assertEquals(10L, match!!.recurringIncome.id)
        assertTrue(match.isSalary)
        assertTrue(match.confidence >= 0.85f)
    }

    @Test
    fun reconcile_rentPayment_matchesRentalIncome() {
        val rentIncome = RecurringIncomeEntity(
            id = 20,
            name = "Flat 302 Rent",
            amount = 18000.0,
            expectedDayOfMonth = 5,
            isSalary = false,
            payerPpiOrBank = "Rahul Sharma"
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 5)
        }

        val tx = TransactionEntity(
            id = 2,
            amount = 18000.0,
            type = "CREDIT",
            merchant = "Rahul Sharma",
            category = "Income",
            rawText = "Received Rs.18,000.00 from Rahul Sharma via UPI for rent",
            timestamp = cal.timeInMillis,
            transactionHash = "hash2"
        )

        val match = RecurringInflowReconciler.reconcile(tx, listOf(rentIncome))
        assertNotNull(match)
        assertEquals(20L, match!!.recurringIncome.id)
        assertEquals(false, match.isSalary)
    }

    @Test
    fun reconcile_debitTransaction_returnsNull() {
        val salaryIncome = RecurringIncomeEntity(
            id = 10,
            name = "Salary",
            amount = 85000.0,
            isSalary = true
        )

        val debitTx = TransactionEntity(
            id = 3,
            amount = 85000.0,
            type = "DEBIT",
            merchant = "Transfer",
            category = "General Expense",
            rawText = "Debited Rs 85000",
            timestamp = System.currentTimeMillis(),
            transactionHash = "hash3"
        )

        val match = RecurringInflowReconciler.reconcile(debitTx, listOf(salaryIncome))
        assertNull(match)
    }
}
