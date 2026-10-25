package com.expensetracker.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.core.database.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {

    @Query("SELECT * FROM credit_cards ORDER BY id ASC")
    fun getAllCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE isActive = 1 ORDER BY id ASC")
    suspend fun getActiveCards(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): CreditCardEntity?

    @Query("SELECT * FROM credit_cards WHERE last4Digits = :last4Digits LIMIT 1")
    suspend fun getCardByLast4Digits(last4Digits: String): CreditCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CreditCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cards: List<CreditCardEntity>)

    @Update
    suspend fun updateCard(card: CreditCardEntity)

    @Delete
    suspend fun deleteCard(card: CreditCardEntity)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAllCards()
}
