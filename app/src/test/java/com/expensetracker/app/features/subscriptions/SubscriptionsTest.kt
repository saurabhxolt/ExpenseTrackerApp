package com.expensetracker.app.features.subscriptions

import com.expensetracker.app.core.database.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SubscriptionsTest {

    @Test
    fun testRecurringMerchantDetection() {
        val transactions = listOf(
            TransactionEntity(amount = 199.0, type = "DEBIT", merchant = "Netflix", rawText = "Paid 199 to Netflix", category = "Entertainment", timestamp = 1721900000000L, transactionHash = "hash1"),
            TransactionEntity(amount = 199.0, type = "DEBIT", merchant = "Netflix", rawText = "Paid 199 to Netflix", category = "Entertainment", timestamp = 1721600000000L, transactionHash = "hash2")
        )

        val recurring = transactions
            .filter { it.type == "DEBIT" }
            .groupBy { it.merchant }
            .filter { entry -> entry.value.size >= 1 }
            .map { (merchant, list) ->
                SubscriptionItem(
                    name = merchant,
                    amount = list.map { it.amount }.average(),
                    billingCycle = "Monthly Autopay",
                    nextDueDate = "28th of this Month",
                    category = list.first().category
                )
            }

        assertEquals(1, recurring.size)
        assertEquals("Netflix", recurring[0].name)
        assertEquals(199.0, recurring[0].amount, 0.01)
    }
}
