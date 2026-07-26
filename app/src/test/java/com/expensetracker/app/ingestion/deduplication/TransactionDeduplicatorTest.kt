package com.expensetracker.app.ingestion.deduplication

import com.expensetracker.app.core.database.dao.TransactionDao
import com.expensetracker.app.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionDeduplicatorTest {

    private class FakeTransactionDao : TransactionDao {
        val storedTransactions = mutableListOf<TransactionEntity>()

        override fun getAllTransactions(): Flow<List<TransactionEntity>> = flowOf(storedTransactions)
        override suspend fun getAllTransactionsList(): List<TransactionEntity> = storedTransactions.toList()
        override fun getTotalExpenses(): Flow<Double?> = flowOf(storedTransactions.filter { it.type == "DEBIT" }.sumOf { it.amount })
        override fun getTotalIncome(): Flow<Double?> = flowOf(storedTransactions.filter { it.type == "CREDIT" }.sumOf { it.amount })

        override suspend fun findCandidateDuplicate(type: String, amount: Double, minTimestamp: Long, maxTimestamp: Long): TransactionEntity? {
            return storedTransactions.firstOrNull {
                it.type == type && Math.abs(it.amount - amount) < 0.01 && it.timestamp in minTimestamp..maxTimestamp
            }
        }

        override suspend fun findExistingCreditCardPayment(amount: Double): TransactionEntity? {
            return storedTransactions.firstOrNull {
                Math.abs(it.amount - amount) < 0.01 &&
                (it.merchant.contains("Credit Card", ignoreCase = true) || it.category == "Bills & Utilities" || it.rawText.contains("credit card", ignoreCase = true))
            }
        }

        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            storedTransactions.add(transaction)
            return storedTransactions.size.toLong()
        }

        override suspend fun updateTransaction(transaction: TransactionEntity) {
            val index = storedTransactions.indexOfFirst { it.id == transaction.id || it.transactionHash == transaction.transactionHash }
            if (index >= 0) {
                storedTransactions[index] = transaction
            }
        }

        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            storedTransactions.remove(transaction)
        }
    }

    @Test
    fun testDuplicateSuppressionWithinWindowForSameMerchant() = runBlocking {
        val dao = FakeTransactionDao()
        val timestamp = System.currentTimeMillis()

        val entity1 = TransactionEntity(
            amount = 250.0,
            type = "DEBIT",
            merchant = "Swiggy",
            rawText = "Paid Rs 250 to Swiggy",
            category = "Food & Dining",
            timestamp = timestamp,
            transactionHash = "hash-1"
        )

        val entity2 = TransactionEntity(
            amount = 250.0,
            type = "DEBIT",
            merchant = "Swiggy",
            rawText = "Debited Rs 250 at Swiggy",
            category = "Food & Dining",
            timestamp = timestamp + 5000L, // 5 seconds later
            transactionHash = "hash-2"
        )

        val id1 = TransactionDeduplicator.insertWithDeduplication(dao, entity1)
        val id2 = TransactionDeduplicator.insertWithDeduplication(dao, entity2)

        assertEquals("First transaction must be inserted", 1L, id1)
        assertEquals("Duplicate transaction within window must be suppressed (-1)", -1L, id2)
        assertEquals("Database should contain only 1 transaction record", 1, dao.storedTransactions.size)
    }

    @Test
    fun testDistinctMerchantsWithSameAmountAllowed() = runBlocking {
        val dao = FakeTransactionDao()
        val timestamp = System.currentTimeMillis()

        val entity1 = TransactionEntity(
            amount = 100.0,
            type = "DEBIT",
            merchant = "Chai Point",
            rawText = "Paid Rs 100 at Chai Point",
            category = "Food & Dining",
            timestamp = timestamp,
            transactionHash = "hash-chai"
        )

        val entity2 = TransactionEntity(
            amount = 100.0,
            type = "DEBIT",
            merchant = "Bakery",
            rawText = "Paid Rs 100 at Bakery",
            category = "Food & Dining",
            timestamp = timestamp + 120000L, // 2 minutes later
            transactionHash = "hash-bakery"
        )

        val id1 = TransactionDeduplicator.insertWithDeduplication(dao, entity1)
        val id2 = TransactionDeduplicator.insertWithDeduplication(dao, entity2)

        assertEquals("Chai Point transaction inserted", 1L, id1)
        assertEquals("Bakery transaction inserted", 2L, id2)
        assertEquals("Database should contain 2 distinct transactions for different merchants", 2, dao.storedTransactions.size)
    }

    @Test
    fun testTimelessCreditCardPaymentConfirmationDeduplication() = runBlocking {
        val dao = FakeTransactionDao()
        val timestamp = System.currentTimeMillis()

        // Bank Debit SMS on Friday
        val bankDebit = TransactionEntity(
            amount = 15000.0,
            type = "DEBIT",
            merchant = "Credit Card Bill",
            rawText = "Rs 15000 debited towards Credit Card XX7478",
            category = "Bills & Utilities",
            timestamp = timestamp,
            transactionHash = "hash-bank-debit"
        )

        // Credit Card Receipt SMS arriving 3 days (72 hours) later
        val cardConfirmation = TransactionEntity(
            amount = 15000.0,
            type = "DEBIT",
            merchant = "Credit Card Bill",
            rawText = "Payment of Rs 15000.00 received towards your HDFC Bank Credit Card XX7478",
            category = "Bills & Utilities",
            timestamp = timestamp + 3 * 24 * 3600 * 1000L, // 3 days later
            transactionHash = "hash-card-confirmation"
        )

        val id1 = TransactionDeduplicator.insertWithDeduplication(dao, bankDebit)
        val id2 = TransactionDeduplicator.insertWithDeduplication(dao, cardConfirmation)

        assertEquals("First bank debit must be inserted", 1L, id1)
        assertEquals("Delayed credit card payment confirmation must be suppressed (-1)", -1L, id2)
        assertEquals("Database should contain exactly 1 transaction record", 1, dao.storedTransactions.size)
    }

    @Test
    fun testFuzzyBrandMerchantDeduplicationZeptoVsZeptoCash() = runBlocking {
        val dao = FakeTransactionDao()
        val timestamp = System.currentTimeMillis()

        val entity1 = TransactionEntity(
            amount = 45.92,
            type = "CREDIT",
            merchant = "Zepto",
            rawText = "Good news! Refund of Rs 45.92 for your Zepto order has been processed.",
            category = "Income",
            timestamp = timestamp,
            transactionHash = "hash-zepto-1"
        )

        val entity2 = TransactionEntity(
            amount = 45.92,
            type = "CREDIT",
            merchant = "Zepto Cash",
            rawText = "Dear Customer, Rs.45.92 has been refunded to your Zepto Cash.",
            category = "Income",
            timestamp = timestamp + 2000L, // 2 seconds later
            transactionHash = "hash-zepto-2"
        )

        val id1 = TransactionDeduplicator.insertWithDeduplication(dao, entity1)
        val id2 = TransactionDeduplicator.insertWithDeduplication(dao, entity2)

        assertEquals("First Zepto refund inserted", 1L, id1)
        assertEquals("Second Zepto Cash refund suppressed as duplicate (-1)", -1L, id2)
        assertEquals("Database should contain 1 transaction record", 1, dao.storedTransactions.size)
        assertEquals("Merchant should be enriched to Zepto Cash", "Zepto Cash", dao.storedTransactions[0].merchant)
    }

    @Test
    fun testDatabaseCleanupExistingDuplicates() = runBlocking {
        val dao = FakeTransactionDao()
        val timestamp = System.currentTimeMillis()

        // Manually populate duplicates into DB
        dao.insertTransaction(
            TransactionEntity(
                id = 1,
                amount = 45.92,
                type = "CREDIT",
                merchant = "Zepto",
                rawText = "Refund of Rs 45.92 processed",
                category = "Income",
                timestamp = timestamp,
                transactionHash = "hash-clean-1"
            )
        )
        dao.insertTransaction(
            TransactionEntity(
                id = 2,
                amount = 45.92,
                type = "CREDIT",
                merchant = "Zepto Cash",
                rawText = "Rs.45.92 refunded to Zepto Cash",
                category = "Income",
                timestamp = timestamp + 2000L,
                transactionHash = "hash-clean-2"
            )
        )

        assertEquals("Initially 2 transactions in DB", 2, dao.storedTransactions.size)

        val deletedCount = TransactionDeduplicator.cleanupExistingDuplicates(dao)

        assertEquals("1 duplicate deleted during cleanup", 1, deletedCount)
        assertEquals("Database cleaned down to 1 transaction record", 1, dao.storedTransactions.size)
    }
}
