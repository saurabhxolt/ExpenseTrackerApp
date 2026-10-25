package com.expensetracker.app.features.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.CategoryRepository
import com.expensetracker.app.core.data.repository.SubscriptionDetector
import com.expensetracker.app.core.data.repository.SubscriptionRepository
import com.expensetracker.app.core.data.repository.TransactionRepository
import com.expensetracker.app.core.data.repository.DetectedSubscriptionCandidate
import com.expensetracker.app.core.database.entity.SubscriptionEntity
import com.expensetracker.app.ingestion.parser.SubscriptionMatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

import com.expensetracker.app.core.analysis.PriceCreepDetector
import com.expensetracker.app.core.analysis.PriceHikeAlert

data class SubscriptionUiModel(
    val entity: SubscriptionEntity,
    val name: String,
    val amount: Double,
    val billingCycle: String,
    val formattedDueDate: String,
    val statusText: String, // e.g. "Due in 3 days", "Due Today", "Overdue by 2 days"
    val isOverdue: Boolean,
    val isDueSoon: Boolean,
    val category: String,
    val isActive: Boolean,
    val autoMatched: Boolean = false,
    val priceHike: PriceHikeAlert? = null
)

data class SubscriptionsUiState(
    val subscriptions: List<SubscriptionUiModel> = emptyList(),
    val detectedCandidates: List<DetectedSubscriptionCandidate> = emptyList(),
    val categories: List<String> = emptyList(),
    val totalMonthlyCost: Double = 0.0,
    val activeCount: Int = 0
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val subscriptionDetector: SubscriptionDetector
) : ViewModel() {

    val uiState: StateFlow<SubscriptionsUiState> = combine(
        subscriptionRepository.allSubscriptions,
        categoryRepository.allCategories,
        transactionRepository.allTransactions
    ) { subs, categories, transactions ->
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val nowMs = System.currentTimeMillis()

        val uiModels = subs.map { entity ->
            val formattedDate = dateFormat.format(Date(entity.nextDueDateMs))

            val diffDays = ((entity.nextDueDateMs - nowMs) / (1000 * 60 * 60 * 24)).toInt()

            val (statusText, isOverdue, isDueSoon) = when {
                entity.autoMatched && entity.lastPaidDateMs != null && (nowMs - entity.lastPaidDateMs) < 7 * 86400000L -> {
                    Triple("Auto-Paid via SMS 🟢 (Next: $formattedDate)", false, false)
                }
                diffDays < 0 -> Triple("Overdue by ${-diffDays} day(s) ($formattedDate)", true, false)
                diffDays == 0 -> Triple("Due Today ($formattedDate)", false, true)
                diffDays <= 5 -> Triple("Due in $diffDays day(s) ($formattedDate)", false, true)
                else -> Triple("Next due: $formattedDate", false, false)
            }

            val priceHike = PriceCreepDetector.detectPriceHikeForSubscription(
                subscriptionName = entity.name,
                currentAmount = entity.amount,
                allTransactions = transactions
            )

            SubscriptionUiModel(
                entity = entity,
                name = entity.name,
                amount = entity.amount,
                billingCycle = entity.billingCycle,
                formattedDueDate = formattedDate,
                statusText = statusText,
                isOverdue = isOverdue,
                isDueSoon = isDueSoon,
                category = entity.category,
                isActive = entity.isActive,
                autoMatched = entity.autoMatched,
                priceHike = priceHike
            )
        }

        val totalMonthly = subs.filter { it.isActive }.sumOf { item ->
            when (item.billingCycle) {
                "Yearly" -> item.amount / 12.0
                "Weekly" -> item.amount * 4.33
                "Quarterly" -> item.amount / 3.0
                else -> item.amount // Monthly
            }
        }

        val candidates = subscriptionDetector.detectCandidates(transactions, subs)
        val catList = (listOf("Subscriptions", "Bills & Utilities", "Rent", "EMI", "Entertainment", "Health & Medical", "Investments", "General Expense") + categories.map { it.name }).distinct()

        SubscriptionsUiState(
            subscriptions = uiModels,
            detectedCandidates = candidates,
            categories = catList,
            totalMonthlyCost = totalMonthly,
            activeCount = subs.count { it.isActive }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubscriptionsUiState()
    )

    fun addDetectedCandidate(candidate: DetectedSubscriptionCandidate) {
        saveSubscription(
            name = candidate.name,
            amount = candidate.amount,
            cycle = candidate.suggestedCycle,
            category = candidate.category,
            dueDateMs = System.currentTimeMillis() + (30L * 24 * 3600 * 1000)
        )
    }

    fun saveSubscription(name: String, amount: Double, cycle: String, category: String, dueDateMs: Long) {
        viewModelScope.launch {
            val entity = SubscriptionEntity(
                name = name,
                amount = amount,
                billingCycle = cycle,
                category = category,
                nextDueDateMs = dueDateMs,
                isActive = true
            )
            subscriptionRepository.saveSubscription(entity)
        }
    }

    fun updateSubscription(entity: SubscriptionEntity, name: String, amount: Double, cycle: String, category: String, dueDateMs: Long) {
        viewModelScope.launch {
            val updated = entity.copy(
                name = name,
                amount = amount,
                billingCycle = cycle,
                category = category,
                nextDueDateMs = dueDateMs
            )
            subscriptionRepository.updateSubscription(updated)
        }
    }

    fun markAsPaid(item: SubscriptionUiModel) {
        viewModelScope.launch {
            val nextDueDate = SubscriptionMatcher.calculateNextDueDate(item.entity.nextDueDateMs, item.billingCycle)
            val updated = item.entity.copy(
                nextDueDateMs = nextDueDate,
                lastPaidDateMs = System.currentTimeMillis(),
                autoMatched = true
            )
            subscriptionRepository.updateSubscription(updated)
        }
    }

    fun toggleSubscriptionActive(entity: SubscriptionEntity) {
        viewModelScope.launch {
            val updated = entity.copy(isActive = !entity.isActive)
            subscriptionRepository.updateSubscription(updated)
        }
    }

    fun deleteSubscription(entity: SubscriptionEntity) {
        viewModelScope.launch {
            subscriptionRepository.deleteSubscription(entity)
        }
    }
}
