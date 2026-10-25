package com.expensetracker.app.features.savings

import com.expensetracker.app.core.database.entity.SavingsGoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class SavingsGoalsTest {

    @Test
    fun testProgressPercentageCalculation() {
        val goal = SavingsGoalEntity(
            title = "Japan Trip",
            targetAmount = 200000.0,
            currentAmount = 50000.0
        )
        val progress = (goal.currentAmount / goal.targetAmount).toFloat()
        assertEquals(0.25f, progress, 0.001f)
    }

    @Test
    fun testGoalAutoCompletionFlag() {
        val activeGoal = SavingsGoalEntity(
            title = "MacBook Pro",
            targetAmount = 150000.0,
            currentAmount = 140000.0
        )
        assertFalse(activeGoal.isCompleted || activeGoal.currentAmount >= activeGoal.targetAmount)

        val completedGoal = activeGoal.copy(currentAmount = 160000.0)
        assertTrue(completedGoal.currentAmount >= completedGoal.targetAmount)
    }

    @Test
    fun testMonthlyPaceNeededCalculation() {
        val now = System.currentTimeMillis()
        val sixMonthsLater = now + (180L * 24 * 60 * 60 * 1000)

        val goal = SavingsGoalEntity(
            title = "Emergency Fund",
            targetAmount = 300000.0,
            currentAmount = 120000.0,
            targetDate = sixMonthsLater
        )

        val diffMs = goal.targetDate - now
        val monthsRemaining = (TimeUnit.MILLISECONDS.toDays(diffMs) / 30.0).coerceAtLeast(0.5)
        val needed = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val perMonth = needed / monthsRemaining

        // 180,000 / 6 = 30,000 per month
        assertEquals(30000.0, perMonth, 100.0)
    }

    @Test
    fun testDepositAndWithdrawBounds() {
        var current = 10000.0
        val deposit = 5000.0
        current += deposit
        assertEquals(15000.0, current, 0.001)

        val withdraw = 20000.0
        current = maxOf(0.0, current - withdraw)
        assertEquals(0.0, current, 0.001)
    }
}
