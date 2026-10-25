package com.expensetracker.app.features.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.SavingsGoalRepository
import com.expensetracker.app.core.database.entity.SavingsGoalEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavingsGoalsUiState(
    val goals: List<SavingsGoalEntity> = emptyList(),
    val totalTargetAmount: Double = 0.0,
    val totalSavedAmount: Double = 0.0,
    val overallProgress: Float = 0f,
    val activeGoalsCount: Int = 0,
    val completedGoalsCount: Int = 0
)

@HiltViewModel
class SavingsGoalsViewModel @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: com.expensetracker.app.core.data.repository.TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<SavingsGoalsUiState> = savingsGoalRepository.allGoals
        .map { goals ->
            val targetSum = goals.sumOf { it.targetAmount }
            val savedSum = goals.sumOf { it.currentAmount }
            val progress = if (targetSum > 0) (savedSum / targetSum).toFloat().coerceIn(0f, 1f) else 0f
            val completed = goals.count { it.isCompleted || (it.targetAmount > 0 && it.currentAmount >= it.targetAmount) }
            val active = goals.size - completed

            SavingsGoalsUiState(
                goals = goals,
                totalTargetAmount = targetSum,
                totalSavedAmount = savedSum,
                overallProgress = progress,
                activeGoalsCount = active,
                completedGoalsCount = completed
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SavingsGoalsUiState()
        )

    fun createGoal(
        title: String,
        targetAmount: Double,
        initialAmount: Double = 0.0,
        targetDate: Long = 0L,
        colorHex: String = "#10B981",
        emoji: String = "🎯",
        note: String = ""
    ) {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                title = title.trim(),
                targetAmount = targetAmount,
                currentAmount = initialAmount,
                targetDate = targetDate,
                colorHex = colorHex,
                emoji = emoji.ifBlank { "🎯" },
                note = note.trim(),
                isCompleted = initialAmount >= targetAmount && targetAmount > 0
            )
            val id = savingsGoalRepository.createGoal(goal)
            if (initialAmount > 0) {
                val now = System.currentTimeMillis()
                transactionRepository.insertTransaction(
                    com.expensetracker.app.core.database.entity.TransactionEntity(
                        amount = initialAmount,
                        type = "INVESTMENT",
                        merchant = "Savings Jar: ${goal.title}",
                        rawText = "Initial deposit into ${goal.title} Jar",
                        category = "Investments",
                        timestamp = now,
                        note = "Initial fund for ${goal.emoji} ${goal.title}",
                        transactionHash = "savings_init_${now}_${goal.title}_$initialAmount",
                        isManual = true
                    )
                )
            }
        }
    }

    fun createStarterTemplate(templateIndex: Int) {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        when (templateIndex) {
            0 -> createGoal("Emergency Fund", 100000.0, 5000.0, now + 180 * dayMs, "#10B981", "🛡️", "Safety buffer for 3-6 months")
            1 -> createGoal("Vacation & Travel", 50000.0, 2000.0, now + 180 * dayMs, "#3B82F6", "🏖️", "Holiday getaway with family/friends")
            2 -> createGoal("Gadget & Tech", 60000.0, 1000.0, now + 90 * dayMs, "#8B5CF6", "💻", "Upgrade phone or computer")
            3 -> createGoal("Vehicle Down Payment", 150000.0, 10000.0, now + 365 * dayMs, "#F59E0B", "🚗", "Car or bike down payment fund")
        }
    }

    fun updateGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            savingsGoalRepository.updateGoal(goal)
        }
    }

    fun deleteGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            savingsGoalRepository.deleteGoal(goal)
        }
    }

    fun depositAmount(goalId: Long, amount: Double, recordInLedger: Boolean = true) {
        viewModelScope.launch {
            val goal = savingsGoalRepository.getGoalById(goalId)
            savingsGoalRepository.depositAmount(goalId, amount)
            if (recordInLedger && goal != null && amount > 0) {
                val now = System.currentTimeMillis()
                transactionRepository.insertTransaction(
                    com.expensetracker.app.core.database.entity.TransactionEntity(
                        amount = amount,
                        type = "INVESTMENT",
                        merchant = "Savings Jar: ${goal.title}",
                        rawText = "Deposit into ${goal.title} Jar",
                        category = "Investments",
                        timestamp = now,
                        note = "Deposited into ${goal.emoji} ${goal.title}",
                        transactionHash = "savings_dep_${now}_${goalId}_$amount",
                        isManual = true
                    )
                )
            }
        }
    }

    fun withdrawAmount(goalId: Long, amount: Double, recordInLedger: Boolean = true) {
        viewModelScope.launch {
            val goal = savingsGoalRepository.getGoalById(goalId)
            savingsGoalRepository.withdrawAmount(goalId, amount)
            if (recordInLedger && goal != null && amount > 0) {
                val now = System.currentTimeMillis()
                transactionRepository.insertTransaction(
                    com.expensetracker.app.core.database.entity.TransactionEntity(
                        amount = amount,
                        type = "CREDIT",
                        merchant = "Savings Jar: ${goal.title}",
                        rawText = "Withdrawal from ${goal.title} Jar",
                        category = "Savings Withdrawal",
                        timestamp = now,
                        note = "Withdrawn from ${goal.emoji} ${goal.title}",
                        transactionHash = "savings_wdr_${now}_${goalId}_$amount",
                        isManual = true
                    )
                )
            }
        }
    }

    fun toggleComplete(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            savingsGoalRepository.toggleComplete(goal.id, !goal.isCompleted)
        }
    }
}
