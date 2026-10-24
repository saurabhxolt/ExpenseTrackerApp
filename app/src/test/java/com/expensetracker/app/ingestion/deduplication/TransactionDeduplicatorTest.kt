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

        override suspend fun getNetBalance(): Double {
            val income = storedTransactions.filter { it.type == "CREDIT" }.sumOf { it.amount }
            val expense = storedTransactions.filter { it.type == "DEBIT" }.sumOf { it.amount }
            return income - expense
        }

        override suspend fun getNetBalanceSince(startTimestamp: Long): Double {
            val income = storedTransactions.filter { it.type == "CREDIT" && it.timestamp >= startTimestamp }.sumOf { it.amount }
            val expense = storedTransactions.filter { it.type == "DEBIT" && it.timestamp >= startTimestamp }.sumOf { it.amount }
            return income - expense
        }

        override suspend fun getTodaySpend(startOfDayTimestamp: Long): Double? {
            val spend = storedTransactions.filter { it.type == "DEBIT" && it.timestamp >= startOfDayTimestamp }.sumOf { it.amount }
            return if (spend > 0) spend else null
        }

        override suspend fun findCandidateDuplicate(type: String, amount: Double): TransactionEntity? {
            return storedTransactions.firstOrNull {
                it.type == type && Math.abs(it.amount - amount) < 0.01
            }
        }

        override suspend fun findDuplicatesInWindow(type: String, amount: Double, timestamp: Long, windowMs: Long): List<TransactionEntity> {
            return storedTransactions.filter {
                it.type == type && Math.abs(it.amount - amount) < 0.01 && Math.abs(it.timestamp - timestamp) <= windowMs
            }.sortedBy { Math.abs(it.timestamp - timestamp) }
        }

        override suspend fun findExistingCreditCardPayment(amount: Double): TransactionEntity? {
            return storedTransactions.firstOrNull {
                Math.abs(it.amount - amount) < 0.01 &&
                (it.merchant.contains("Credit Card", ignoreCase = true) || it.merchant.contains("Card Bill", ignoreCase = true) || it.type == "TRANSFER" || it.category == "Bills & Utilities" || it.rawText.contains("credit card", ignoreCase = true) || it.rawText.contains("sbi card", ignoreCase = true) || it.rawText.contains("bobcard", ignoreCase = true) || it.rawText.contains("onecard", ignoreCase = true) || it.rawText.contains("cheq", ignoreCase = true) || it.rawText.contains("cred", ignoreCase = true))
            }
        }

        override suspend fun findCreditCardPaymentsInWindow(amount: Double, timestamp: Long, windowMs: Long): List<TransactionEntity> {
            return storedTransactions.filter {
                Math.abs(it.amount - amount) < 0.01 && Math.abs(it.timestamp - timestamp) <= windowMs &&
                (it.merchant.contains("Credit Card", ignoreCase = true) || it.merchant.contains("Card Bill", ignoreCase = true) || it.type == "TRANSFER" || it.category == "Bills & Utilities" || it.rawText.contains("credit card", ignoreCase = true) || it.rawText.contains("sbi card", ignoreCase = true) || it.rawText.contains("bobcard", ignoreCase = true) || it.rawText.contains("onecard", ignoreCase = true) || it.rawText.contains("cheq", ignoreCase = true) || it.rawText.contains("cred", ignoreCase = true))
            }.sortedBy { Math.abs(it.timestamp - timestamp) }
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

        override suspend fun updateTransactionCategoryName(oldCategoryName: String, newCategoryName: String) {
            storedTransactions.forEachIndexed { index, tx ->
                if (tx.category == oldCategoryName) {
                    storedTransactions[index] = tx.copy(category = newCategoryName)
                }
            }
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

    @Test
    fun testCheQVsCreditCardBillDeduplication() = runBlocking {
        val dao = FakeTransactionDao()
        val timestamp = System.currentTimeMillis()

        val entity1 = TransactionEntity(
            amount = 18681.0,
            type = "DEBIT",
            merchant = "CheQ",
            rawText = "Rs 18681.00 debited for payment to CheQ",
            category = "General Expense",
            timestamp = timestamp,
            transactionHash = "hash-cheq-1"
        )

        val entity2 = TransactionEntity(
            amount = 18681.0,
            type = "DEBIT",
            merchant = "Credit Card Bill",
            rawText = "Payment of Rs 18681.00 received towards your Credit Card",
            category = "Bills & Utilities",
            timestamp = timestamp + 20 * 60 * 1000L, // 20 minutes later
            transactionHash = "hash-card-2"
        )

        val id1 = TransactionDeduplicator.insertWithDeduplication(dao, entity1)
        val id2 = TransactionDeduplicator.insertWithDeduplication(dao, entity2)

        assertEquals("First CheQ payment inserted", 1L, id1)
        assertEquals("Second Credit Card Bill payment suppressed as duplicate (-1)", -1L, id2)
        assertEquals("Database should contain 1 transaction record", 1, dao.storedTransactions.size)
    }

    @Test
    fun testAckoInsurance12MinWindowDeduplication() = runBlocking {
        val dao = FakeTransactionDao()
        val timestamp = System.currentTimeMillis()

        val entity1 = TransactionEntity(
            amount = 5080.0,
            type = "DEBIT",
            merchant = "ACKO Insurance",
            rawText = "Paid Rs 5080 to ACKO Insurance",
            category = "Insurance",
            timestamp = timestamp,
            transactionHash = "hash-acko-1"
        )

        val entity2 = TransactionEntity(
            amount = 5080.0,
            type = "DEBIT",
            merchant = "ACKO Insurance",
            rawText = "We received payment of Rs 5080 for your bike insurance policy",
            category = "Insurance",
            timestamp = timestamp + 12 * 60 * 1000L, // 12 minutes later
            transactionHash = "hash-acko-2"
        )

        val id1 = TransactionDeduplicator.insertWithDeduplication(dao, entity1)
        val id2 = TransactionDeduplicator.insertWithDeduplication(dao, entity2)

        assertEquals("First ACKO payment inserted", 1L, id1)
        assertEquals("Second ACKO receipt 12 minutes later suppressed as duplicate (-1)", -1L, id2)
        assertEquals("Database should contain 1 transaction record", 1, dao.storedTransactions.size)
    }

    @Test
    fun testAirtelUpiVsAirtelBlackDeduplication() = runBlocking {
        val dao = FakeTransactionDao()
        val timestamp = System.currentTimeMillis()

        val entity1 = TransactionEntity(
            amount = 347.16,
            type = "DEBIT",
            merchant = "Airtel",
            rawText = "Paid Rs 347.16 via UPI to Upi-airtel Payments Bank",
            category = "Bills & Utilities",
            timestamp = timestamp,
            transactionHash = "hash-airtel-1"
        )

        val entity2 = TransactionEntity(
            amount = 347.16,
            type = "DEBIT",
            merchant = "Airtel Black",
            rawText = "Hi Saurabh, we have received a payment of Rs. 347.16 for your Airtel Black ID",
            category = "Bills & Utilities",
            timestamp = timestamp + 4 * 60 * 1000L, // 4 minutes later
            transactionHash = "hash-airtel-2"
        )

        val id1 = TransactionDeduplicator.insertWithDeduplication(dao, entity1)
        val id2 = TransactionDeduplicator.insertWithDeduplication(dao, entity2)

        assertEquals("First Airtel UPI payment inserted", 1L, id1)
        assertEquals("Second Airtel Black receipt suppressed as duplicate (-1)", -1L, id2)
        assertEquals("Database should contain 1 transaction record", 1, dao.storedTransactions.size)
    }

    @Test
    fun testLegitimateRepeatPurchaseOutsideWindowAllowed() = runBlocking {
        val dao = FakeTransactionDao()
        val timestamp = System.currentTimeMillis()

        // First purchase: Starbucks coffee ₹250 today
        val coffee1 = TransactionEntity(
            amount = 250.0,
            type = "DEBIT",
            merchant = "Starbucks",
            rawText = "Paid Rs 250 at Starbucks",
            category = "Food & Dining",
            timestamp = timestamp,
            transactionHash = "hash-starbucks-1"
        )

        // Second purchase: Starbucks coffee ₹250 three days later
        val coffee2 = TransactionEntity(
            amount = 250.0,
            type = "DEBIT",
            merchant = "Starbucks",
            rawText = "Paid Rs 250 at Starbucks",
            category = "Food & Dining",
            timestamp = timestamp + 3 * 24 * 3600 * 1000L, // 3 days later
            transactionHash = "hash-starbucks-2"
        )

        val id1 = TransactionDeduplicator.insertWithDeduplication(dao, coffee1)
        val id2 = TransactionDeduplicator.insertWithDeduplication(dao, coffee2)

        assertEquals("First coffee purchase inserted", 1L, id1)
        assertEquals("Second coffee purchase on another day MUST be inserted (not suppressed)", 2L, id2)
        assertEquals("Database must contain 2 separate records for repeat purchases", 2, dao.storedTransactions.size)
    }
}
