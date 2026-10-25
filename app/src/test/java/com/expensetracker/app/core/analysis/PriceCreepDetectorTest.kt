package com.expensetracker.app.core.analysis

import com.expensetracker.app.core.database.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PriceCreepDetectorTest {

    private fun testTransaction(
        id: Long,
        merchant: String,
        amount: Double,
        timestamp: Long,
        type: String = "DEBIT"
    ) = TransactionEntity(
        id = id,
        merchant = merchant,
        amount = amount,
        type = type,
        rawText = "Paid Rs.$amount to $merchant",
        category = "Entertainment",
        timestamp = timestamp,
        transactionHash = "hash_$id"
    )

    @Test
    fun detectPriceHikeForTransaction_flagsSignificantHike() {
        val oldTx = testTransaction(
            id = 1,
            merchant = "Netflix India Pvt Ltd",
            amount = 649.0,
            timestamp = 1000L
        )
        val newTx = testTransaction(
            id = 2,
            merchant = "Netflix",
            amount = 799.0,
            timestamp = 2000L
        )

        val hike = PriceCreepDetector.detectPriceHikeForTransaction(newTx, listOf(oldTx, newTx))
        assertNotNull(hike)
        assertEquals(150.0, hike!!.difference, 0.01)
        assertEquals(23.1, hike.percentageIncrease, 0.1)
        assertEquals(649.0, hike.previousAmount, 0.01)
        assertEquals(799.0, hike.currentAmount, 0.01)
    }

    @Test
    fun detectPriceHikeForTransaction_ignoresMinorFluctuation() {
        val oldTx = testTransaction(
            id = 1,
            merchant = "Spotify",
            amount = 119.0,
            timestamp = 1000L
        )
        // Only 1 rupee increase (< 3%)
        val newTx = testTransaction(
            id = 2,
            merchant = "Spotify",
            amount = 120.0,
            timestamp = 2000L
        )

        val hike = PriceCreepDetector.detectPriceHikeForTransaction(newTx, listOf(oldTx, newTx))
        assertNull(hike)
    }

    @Test
    fun detectPriceHikeForSubscription_detectsHikeAgainstPriorDebits() {
        val debits = listOf(
            testTransaction(id = 1, merchant = "Airtel Broadband", amount = 999.0, timestamp = 1000L),
            testTransaction(id = 2, merchant = "Airtel Wi-Fi", amount = 1199.0, timestamp = 2000L)
        )

        val hike = PriceCreepDetector.detectPriceHikeForSubscription(
            subscriptionName = "Airtel Broadband",
            currentAmount = 1199.0,
            allTransactions = debits
        )

        assertNotNull(hike)
        assertEquals(200.0, hike!!.difference, 0.01)
        assertEquals(20.0, hike.percentageIncrease, 0.1)
        assertEquals(999.0, hike.previousAmount, 0.01)
        assertEquals(1199.0, hike.currentAmount, 0.01)
    }

    @Test
    fun normalizeMerchant_stripsSuffixesAndPunctuation() {
        assertEquals("netflix", PriceCreepDetector.normalizeMerchant("Netflix India Pvt Ltd"))
        assertEquals("spotify", PriceCreepDetector.normalizeMerchant("Spotify App"))
        assertEquals("airtel", PriceCreepDetector.normalizeMerchant("Airtel Solutions Ltd"))
    }
}
