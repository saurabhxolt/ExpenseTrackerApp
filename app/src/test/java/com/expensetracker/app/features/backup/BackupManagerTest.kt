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

    @Test
    fun testBackupJsonWithSplitEntitiesRoundtrip() {
        val groups = listOf(
            com.expensetracker.app.core.database.entity.GroupEntity(
                id = 1L,
                name = "Goa Trip",
                description = "Vacation expenses",
                emoji = "🏖️"
            )
        )
        val members = listOf(
            com.expensetracker.app.core.database.entity.GroupMemberEntity(
                id = 1L,
                groupId = 1L,
                memberName = "Alice"
            )
        )
        val expenses = listOf(
            com.expensetracker.app.core.database.entity.SplitExpenseEntity(
                id = 10L,
                groupId = 1L,
                title = "Dinner at Fisherman's Wharf",
                totalAmount = 2400.0,
                paidByMember = "Alice"
            )
        )
        val shares = listOf(
            com.expensetracker.app.core.database.entity.ExpenseSplitShareEntity(
                id = 100L,
                expenseId = 10L,
                memberName = "Alice",
                shareAmount = 1200.0
            )
        )
        val settlements = listOf(
            com.expensetracker.app.core.database.entity.GroupSettlementEntity(
                id = 5L,
                groupId = 1L,
                fromMember = "Bob",
                toMember = "Alice",
                amount = 1200.0
            )
        )

        val json = BackupManager.createBackupJson(
            transactions = emptyList(),
            categories = emptyList(),
            budgets = emptyList(),
            groups = groups,
            groupMembers = members,
            splitExpenses = expenses,
            splitShares = shares,
            groupSettlements = settlements
        )

        val restored = BackupManager.parseBackupData(json)

        assertEquals(1, restored.groups.size)
        assertEquals("Goa Trip", restored.groups[0].name)
        assertEquals("🏖️", restored.groups[0].emoji)

        assertEquals(1, restored.groupMembers.size)
        assertEquals("Alice", restored.groupMembers[0].memberName)

        assertEquals(1, restored.splitExpenses.size)
        assertEquals("Dinner at Fisherman's Wharf", restored.splitExpenses[0].title)
        assertEquals(2400.0, restored.splitExpenses[0].totalAmount, 0.01)

        assertEquals(1, restored.splitShares.size)
        assertEquals(1200.0, restored.splitShares[0].shareAmount, 0.01)

        assertEquals(1, restored.groupSettlements.size)
        assertEquals("Bob", restored.groupSettlements[0].fromMember)
        assertEquals("Alice", restored.groupSettlements[0].toMember)
        assertEquals(1200.0, restored.groupSettlements[0].amount, 0.01)
    }
}
