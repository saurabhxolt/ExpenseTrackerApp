package com.expensetracker.app.features.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.analysis.CreditCardRadarCalculator
import com.expensetracker.app.core.analysis.CreditCardRadarStatus
import com.expensetracker.app.core.analysis.DiscoveredCreditCard
import com.expensetracker.app.core.data.repository.CreditCardRepository
import com.expensetracker.app.core.database.dao.TransactionDao
import com.expensetracker.app.core.database.entity.CreditCardEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreditCardRadarUiState(
    val cardStatuses: List<CreditCardRadarStatus> = emptyList(),
    val discoveredCards: List<DiscoveredCreditCard> = emptyList(),
    val totalOutstandingAllCards: Double = 0.0,
    val totalLimitAllCards: Double = 0.0,
    val aggregateUtilization: Double = 0.0,
    val isLoading: Boolean = false
)

@HiltViewModel
class CreditCardViewModel @Inject constructor(
    private val creditCardRepository: CreditCardRepository,
    private val transactionDao: TransactionDao
) : ViewModel() {

    val uiState: StateFlow<CreditCardRadarUiState> = combine(
        creditCardRepository.allCards,
        transactionDao.getAllTransactions()
    ) { cards, transactions ->
        val statuses = cards.map { card ->
            CreditCardRadarCalculator.analyzeCard(card, transactions)
        }

        val discovered = CreditCardRadarCalculator.discoverUnregisteredCards(transactions, cards)

        val totalOutstanding = statuses.sumOf { it.currentOutstanding }
        val totalLimit = statuses.sumOf { it.card.totalLimit }
        val aggregateUtil = if (totalLimit > 0) (totalOutstanding / totalLimit) * 100.0 else 0.0

        CreditCardRadarUiState(
            cardStatuses = statuses,
            discoveredCards = discovered,
            totalOutstandingAllCards = totalOutstanding,
            totalLimitAllCards = totalLimit,
            aggregateUtilization = aggregateUtil,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreditCardRadarUiState(isLoading = true)
    )

    fun addCard(
        cardName: String,
        last4Digits: String,
        totalLimit: Double,
        statementDayOfMonth: Int,
        dueDayOfMonth: Int,
        bankName: String,
        cardColorHex: Long = 0xFF1E3A8A
    ) {
        viewModelScope.launch {
            val card = CreditCardEntity(
                cardName = cardName.trim().ifEmpty { "$bankName Card" },
                last4Digits = last4Digits.trim(),
                totalLimit = totalLimit,
                statementDayOfMonth = statementDayOfMonth.coerceIn(1, 31),
                dueDayOfMonth = dueDayOfMonth.coerceIn(1, 31),
                cardColorHex = cardColorHex,
                bankName = bankName.trim().ifEmpty { "Bank" }
            )
            creditCardRepository.insertCard(card)
        }
    }

    fun updateCard(card: CreditCardEntity) {
        viewModelScope.launch {
            creditCardRepository.updateCard(card)
        }
    }

    fun deleteCard(card: CreditCardEntity) {
        viewModelScope.launch {
            creditCardRepository.deleteCard(card)
        }
    }

    fun onboardDiscoveredCard(
        discovered: DiscoveredCreditCard,
        totalLimit: Double,
        statementDayOfMonth: Int,
        dueDayOfMonth: Int
    ) {
        addCard(
            cardName = discovered.suggestedName,
            last4Digits = discovered.last4Digits,
            totalLimit = totalLimit,
            statementDayOfMonth = statementDayOfMonth,
            dueDayOfMonth = dueDayOfMonth,
            bankName = discovered.suggestedBank
        )
    }
}
