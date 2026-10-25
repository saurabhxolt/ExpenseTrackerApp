package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.InsurancePolicyDao
import com.expensetracker.app.core.database.dao.SavingsGoalDao
import com.expensetracker.app.core.database.entity.InsurancePolicyEntity
import com.expensetracker.app.core.database.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsurancePolicyRepository @Inject constructor(
    private val insurancePolicyDao: InsurancePolicyDao,
    private val savingsGoalDao: SavingsGoalDao
) {
    val allPolicies: Flow<List<InsurancePolicyEntity>> = insurancePolicyDao.getAllPolicies()

    suspend fun getAllPoliciesList(): List<InsurancePolicyEntity> = insurancePolicyDao.getAllPoliciesList()

    suspend fun insertPolicy(policy: InsurancePolicyEntity): Long = insurancePolicyDao.insertPolicy(policy)

    suspend fun updatePolicy(policy: InsurancePolicyEntity) = insurancePolicyDao.updatePolicy(policy)

    suspend fun deletePolicy(policy: InsurancePolicyEntity) = insurancePolicyDao.deletePolicy(policy)

    suspend fun createPolicyWithSinkingJar(
        policy: InsurancePolicyEntity,
        createJar: Boolean,
        jarColorHex: String = "#3B82F6",
        jarEmoji: String = ""
    ): Long {
        var linkedJarId = policy.linkedSavingsGoalId
        if (createJar && linkedJarId == null) {
            val emoji = if (jarEmoji.isNotBlank()) jarEmoji else getEmojiForPolicyType(policy.policyType)
            val jar = SavingsGoalEntity(
                title = "${policy.policyName} Jar",
                targetAmount = policy.premiumAmount,
                currentAmount = 0.0,
                targetDate = policy.renewalDueDateMs,
                colorHex = jarColorHex,
                emoji = emoji,
                note = "Auto-linked for ${policy.policyName} renewal",
                isCompleted = false
            )
            linkedJarId = savingsGoalDao.insertGoal(jar)
        }
        val policyToSave = policy.copy(linkedSavingsGoalId = linkedJarId)
        return insurancePolicyDao.insertPolicy(policyToSave)
    }

    suspend fun markPolicyRenewed(policy: InsurancePolicyEntity) {
        val nextDue = calculateNextRenewalDate(policy.renewalDueDateMs, policy.frequency)
        val updated = policy.copy(
            lastPaidDateMs = System.currentTimeMillis(),
            renewalDueDateMs = nextDue
        )
        insurancePolicyDao.updatePolicy(updated)
    }

    companion object {
        fun getEmojiForPolicyType(type: String): String {
            return when (type.uppercase()) {
                "HEALTH" -> "🏥"
                "TERM" -> "🛡️"
                "CAR" -> "🚗"
                "BIKE" -> "🏍️"
                "LIC" -> "📜"
                else -> "📑"
            }
        }

        fun calculateNextRenewalDate(currentDueDateMs: Long, frequency: String): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = if (currentDueDateMs > 0) currentDueDateMs else System.currentTimeMillis()
            when (frequency.lowercase()) {
                "quarterly" -> cal.add(Calendar.MONTH, 3)
                "half-yearly", "semi-annually" -> cal.add(Calendar.MONTH, 6)
                "yearly", "annually" -> cal.add(Calendar.YEAR, 1)
                else -> cal.add(Calendar.YEAR, 1)
            }
            return cal.timeInMillis
        }
    }
}
