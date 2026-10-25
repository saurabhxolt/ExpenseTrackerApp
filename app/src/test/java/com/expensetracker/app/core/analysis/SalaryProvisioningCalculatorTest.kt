package com.expensetracker.app.core.analysis

import com.expensetracker.app.core.database.entity.InsurancePolicyEntity
import com.expensetracker.app.core.database.entity.SavingsGoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SalaryProvisioningCalculatorTest {

    @Test
    fun calculatePlan_withDeficit_computesCorrectMonthlyPace() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 1, 10, 0, 0)
        }.timeInMillis

        // Policy due 6 months later (April 2027), premium 24,000, current jar has 6,000
        val renewalDue = Calendar.getInstance().apply {
            set(2027, Calendar.APRIL, 1, 10, 0, 0)
        }.timeInMillis

        val policy = InsurancePolicyEntity(
            id = 1,
            policyName = "HDFC Ergo Car Insurance",
            policyType = "CAR",
            premiumAmount = 24000.0,
            renewalDueDateMs = renewalDue,
            linkedSavingsGoalId = 101L
        )

        val jar = SavingsGoalEntity(
            id = 101,
            title = "Car Insurance Jar",
            targetAmount = 24000.0,
            currentAmount = 6000.0,
            targetDate = renewalDue,
            colorHex = "#3B82F6",
            emoji = "🚗",
            note = ""
        )

        val plan = SalaryProvisioningCalculator.calculatePlan(
            policies = listOf(policy),
            jars = listOf(jar),
            nowMs = now
        )

        assertEquals(1, plan.items.size)
        val item = plan.items[0]
        assertEquals(18000.0, item.deficitAmount, 0.01)
        assertEquals(6, item.monthsRemaining)
        // 18,000 deficit over 6 months = 3,000/month recommended stash
        assertEquals(3000.0, item.recommendedMonthlyStash, 0.01)
        assertFalse(item.isFunded)
        assertEquals(3000.0, plan.totalRecommendedStash, 0.01)
    }

    @Test
    fun calculatePlan_fullyFunded_hasZeroRecommendedStash() {
        val now = System.currentTimeMillis()
        val renewalDue = now + 90L * 24 * 60 * 60 * 1000 // 90 days

        val policy = InsurancePolicyEntity(
            id = 2,
            policyName = "Star Health Mediclaim",
            policyType = "HEALTH",
            premiumAmount = 15000.0,
            renewalDueDateMs = renewalDue,
            linkedSavingsGoalId = 202L
        )

        val jar = SavingsGoalEntity(
            id = 202,
            title = "Health Jar",
            targetAmount = 15000.0,
            currentAmount = 15000.0, // Fully saved!
            targetDate = renewalDue,
            colorHex = "#10B981",
            emoji = "🏥",
            note = ""
        )

        val plan = SalaryProvisioningCalculator.calculatePlan(
            policies = listOf(policy),
            jars = listOf(jar),
            nowMs = now
        )

        val item = plan.items[0]
        assertEquals(0.0, item.deficitAmount, 0.01)
        assertEquals(0.0, item.recommendedMonthlyStash, 0.01)
        assertTrue(item.isFunded)
        assertEquals(0.0, plan.totalRecommendedStash, 0.01)
    }
}
