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
    private val savingsGoalRepository: SavingsGoalRepository
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
            savingsGoalRepository.createGoal(goal)
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

    fun depositAmount(goalId: Long, amount: Double) {
        viewModelScope.launch {
            savingsGoalRepository.depositAmount(goalId, amount)
        }
    }

    fun withdrawAmount(goalId: Long, amount: Double) {
        viewModelScope.launch {
            savingsGoalRepository.withdrawAmount(goalId, amount)
        }
    }

    fun toggleComplete(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            savingsGoalRepository.toggleComplete(goal.id, !goal.isCompleted)
        }
    }
}
