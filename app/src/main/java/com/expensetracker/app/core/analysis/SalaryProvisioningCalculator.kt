package com.expensetracker.app.core.analysis

import com.expensetracker.app.core.database.entity.InsurancePolicyEntity
import com.expensetracker.app.core.database.entity.SavingsGoalEntity
import java.util.Calendar
import kotlin.math.max

data class PolicyProvisioningItem(
    val policy: InsurancePolicyEntity,
    val linkedJar: SavingsGoalEntity?,
    val monthsRemaining: Int,
    val daysRemaining: Int,
    val currentSavedAmount: Double,
    val targetAmount: Double,
    val deficitAmount: Double,
    val recommendedMonthlyStash: Double,
    val isFunded: Boolean,
    val isDueSoon: Boolean // due within 30 days
)

data class SalaryProvisioningPlan(
    val items: List<PolicyProvisioningItem> = emptyList(),
    val totalRecommendedStash: Double = 0.0,
    val totalAnnualPremiums: Double = 0.0,
    val activePolicyCount: Int = 0,
    val urgentCount: Int = 0
)

object SalaryProvisioningCalculator {

    fun calculatePlan(
        policies: List<InsurancePolicyEntity>,
        jars: List<SavingsGoalEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): SalaryProvisioningPlan {
        val activePolicies = policies.filter { it.isActive }
        val jarsById = jars.associateBy { it.id }

        val items = activePolicies.map { policy ->
            val linkedJar = policy.linkedSavingsGoalId?.let { jarsById[it] }
            val currentSaved = linkedJar?.currentAmount ?: 0.0
            val target = policy.premiumAmount
            val deficit = max(0.0, target - currentSaved)

            val diffMs = policy.renewalDueDateMs - nowMs
            val daysRemaining = max(0, (diffMs / (1000L * 60 * 60 * 24)).toInt())
            
            // Calculate calendar months remaining
            val monthsRemaining = calculateMonthsDifference(nowMs, policy.renewalDueDateMs)
            
            // Safe division: if due this month or overdue, entire deficit is recommended now
            val recommendedStash = when {
                deficit <= 0.0 -> 0.0
                monthsRemaining <= 1 -> deficit
                else -> Math.round((deficit / monthsRemaining) * 100.0) / 100.0
            }

            val isFunded = deficit <= 0.0
            val isDueSoon = daysRemaining in 0..30

            PolicyProvisioningItem(
                policy = policy,
                linkedJar = linkedJar,
                monthsRemaining = monthsRemaining,
                daysRemaining = daysRemaining,
                currentSavedAmount = currentSaved,
                targetAmount = target,
                deficitAmount = deficit,
                recommendedMonthlyStash = recommendedStash,
                isFunded = isFunded,
                isDueSoon = isDueSoon
            )
        }.sortedWith(
            compareBy<PolicyProvisioningItem> { it.isFunded }
                .thenBy { it.daysRemaining }
        )

        val totalStash = items.sumOf { it.recommendedMonthlyStash }
        val totalPremiums = items.sumOf { it.targetAmount }
        val urgent = items.count { it.isDueSoon && !it.isFunded }

        return SalaryProvisioningPlan(
            items = items,
            totalRecommendedStash = Math.round(totalStash * 100.0) / 100.0,
            totalAnnualPremiums = Math.round(totalPremiums * 100.0) / 100.0,
            activePolicyCount = items.size,
            urgentCount = urgent
        )
    }

    private fun calculateMonthsDifference(fromMs: Long, toMs: Long): Int {
        if (toMs <= fromMs) return 1
        val c1 = Calendar.getInstance().apply { timeInMillis = fromMs }
        val c2 = Calendar.getInstance().apply { timeInMillis = toMs }
        val yearDiff = c2.get(Calendar.YEAR) - c1.get(Calendar.YEAR)
        val monthDiff = c2.get(Calendar.MONTH) - c1.get(Calendar.MONTH)
        val totalMonths = yearDiff * 12 + monthDiff
        return max(1, totalMonths)
    }
}
