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
            CategoryEntity(name = "Food & Dining", type = "EXPENSE", iconName = "Food", colorHex = "#FF5722", isCustom = true, emoji = "🍕")
        )
        val budgets = listOf(
            BudgetEntity(categoryName = "Food & Dining", limitAmount = 10000.0, period = "MONTHLY")
        )
        val subscriptions = listOf(
            com.expensetracker.app.core.database.entity.SubscriptionEntity(
                name = "Netflix",
                amount = 649.0,
                billingCycle = "Monthly",
                nextDueDateMs = 1725000000000L,
                category = "Subscriptions",
                isActive = true,
                autoMatched = true,
                lastPaidDateMs = 1722000000000L
            )
        )

        val jsonStr = BackupManager.createBackupJson(
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            subscriptions = subscriptions
        )
        assertNotNull(jsonStr)

        val restored = BackupManager.parseBackupData(jsonStr)

        assertEquals(1, restored.transactions.size)
        assertEquals("Swiggy", restored.transactions[0].merchant)
        assertEquals(450.00, restored.transactions[0].amount, 0.01)

        assertEquals(1, restored.categories.size)
        assertEquals("Food & Dining", restored.categories[0].name)
        assertEquals("🍕", restored.categories[0].emoji)
        assertEquals(true, restored.categories[0].isCustom)

        assertEquals(1, restored.budgets.size)
        assertEquals(10000.0, restored.budgets[0].limitAmount, 0.01)
        assertEquals("MONTHLY", restored.budgets[0].period)

        assertEquals(1, restored.subscriptions.size)
        assertEquals("Netflix", restored.subscriptions[0].name)
        assertEquals(649.0, restored.subscriptions[0].amount, 0.01)
        assertEquals(true, restored.subscriptions[0].autoMatched)
        assertEquals(1722000000000L, restored.subscriptions[0].lastPaidDateMs)
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
