package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.CreditCardDao
import com.expensetracker.app.core.database.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreditCardRepository @Inject constructor(
    private val creditCardDao: CreditCardDao
) {
    val allCards: Flow<List<CreditCardEntity>> = creditCardDao.getAllCards()

    suspend fun getActiveCards(): List<CreditCardEntity> {
        return creditCardDao.getActiveCards()
    }

    suspend fun getCardById(id: Long): CreditCardEntity? {
        return creditCardDao.getCardById(id)
    }

    suspend fun getCardByLast4Digits(last4Digits: String): CreditCardEntity? {
        return creditCardDao.getCardByLast4Digits(last4Digits)
    }

    suspend fun insertCard(card: CreditCardEntity): Long {
        return creditCardDao.insertCard(card)
    }

    suspend fun updateCard(card: CreditCardEntity) {
        creditCardDao.updateCard(card)
    }

    suspend fun deleteCard(card: CreditCardEntity) {
        creditCardDao.deleteCard(card)
    }

    suspend fun deleteCardById(id: Long) {
        creditCardDao.deleteCardById(id)
    }
}
