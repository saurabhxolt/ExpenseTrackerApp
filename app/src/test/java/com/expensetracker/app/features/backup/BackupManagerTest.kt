package com.expensetracker.app.features.backup

import com.expensetracker.app.core.database.entity.BudgetEntity
import com.expensetracker.app.core.database.entity.CategoryEntity
import com.expensetracker.app.core.database.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BackupManagerTest {

    @Test
    fun testBackupJsonExportAndImportRoundtrip() {
        val transactions = listOf(
            TransactionEntity(
                amount = 450.00,
                type = "DEBIT",
                merchant = "Swiggy",
                rawText = "Paid Rs 450 to Swiggy",
                category = "Food & Dining",
                timestamp = 1721900000000L,
                transactionHash = "Swiggy-450.0-1721900000000"
            )
        )
        val categories = listOf(
            CategoryEntity(name = "Food & Dining", type = "EXPENSE", iconName = "Food", colorHex = "#FF5722")
        )
        val budgets = listOf(
            BudgetEntity(categoryName = "Food & Dining", limitAmount = 10000.0)
        )

        val jsonStr = BackupManager.createBackupJson(transactions, categories, budgets)
        assertNotNull(jsonStr)

        val (importedTrx, importedCats, importedBgts) = BackupManager.parseBackupJson(jsonStr)

        assertEquals(1, importedTrx.size)
        assertEquals("Swiggy", importedTrx[0].merchant)
        assertEquals(450.00, importedTrx[0].amount, 0.01)

        assertEquals(1, importedCats.size)
        assertEquals("Food & Dining", importedCats[0].name)

        assertEquals(1, importedBgts.size)
        assertEquals(10000.0, importedBgts[0].limitAmount, 0.01)
    }
}
