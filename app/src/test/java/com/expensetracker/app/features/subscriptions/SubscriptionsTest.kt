package com.expensetracker.app.features.subscriptions

import com.expensetracker.app.core.database.entity.SubscriptionEntity
import com.expensetracker.app.ingestion.parser.SubscriptionDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionsTest {

    @Test
    fun testSubscriptionDetectorKeywords() {
        assertTrue(SubscriptionDetector.isRecurringMandateText("Rs. 649 debited for Netflix autopay subscription"))
        assertTrue(SubscriptionDetector.isRecurringMandateText("Standing instruction executed for Rent payment Rs 25000"))
        assertTrue(SubscriptionDetector.isRecurringMandateText("SIP debit of Rs 5000 successful"))
    }

    @Test
    fun testSubscriptionEntityCreation() {
        val sub = SubscriptionEntity(
            name = "Netflix",
            amount = 649.0,
            billingCycle = "Monthly",
            category = "Entertainment"
        )
        assertEquals("Netflix", sub.name)
        assertEquals(649.0, sub.amount, 0.01)
        assertEquals("Monthly", sub.billingCycle)
        assertTrue(sub.isActive)
    }
}
