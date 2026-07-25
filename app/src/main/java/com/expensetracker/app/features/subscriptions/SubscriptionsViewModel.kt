package com.expensetracker.app.features.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SubscriptionItem(
    val name: String,
    val amount: Double,
    val billingCycle: String,
    val nextDueDate: String,
    val category: String
)

data class SubscriptionsUiState(
    val subscriptions: List<SubscriptionItem> = emptyList(),
    val totalMonthlyCost: Double = 0.0
)

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<SubscriptionsUiState> = transactionRepository.allTransactions.map { transactions ->
        // Identify recurring merchant names (merchants appearing > 1 time)
        val recurringMerchants = transactions
            .filter { it.type == "DEBIT" }
            .groupBy { it.merchant }
            .filter { entry -> entry.value.size >= 1 }
            .map { (merchant, list) ->
                val avgAmount = list.map { it.amount }.average()
                SubscriptionItem(
                    name = merchant,
                    amount = avgAmount,
                    billingCycle = "Monthly Autopay",
                    nextDueDate = "28th of this Month",
                    category = list.first().category
                )
            }

        val totalCost = recurringMerchants.sumOf { it.amount }

        SubscriptionsUiState(
            subscriptions = recurringMerchants,
            totalMonthlyCost = totalCost
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubscriptionsUiState()
    )
}
