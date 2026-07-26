package com.expensetracker.app.ingestion.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RegexTransactionParserTest {

    @Test
    fun testExcludeAccountBalanceInfoMsg() {
        val sms = "Available Bal in HDFC Bank A/c XX3856 as on yesterday:02-JUL-26 is INR 72,021.41. Cheques are subject to clearing.For updated A/C Bal dial 18002703333."
        val result = RegexTransactionParser.parse(sms)
        assertNull("Should exclude pure balance info messages", result)
    }

    @Test
    fun testExcludeEmandateUpcomingDeduction() {
        val sms = "E-Mandate! Rs.1955.00 will be deducted on 25/07/26, 00:00:00 For ICCL Mutual Funds Autopay mandate UMN 4775711caee946cfaf42cdfcc37e02b2@ybl Maintain Balance -HDFC Bank"
        val result = RegexTransactionParser.parse(sms)
        assertNull("Should exclude future e-mandate upcoming deduction alerts", result)
    }

    @Test
    fun testHdfcTxnUpiVpa() {
        val sms = "Txn Rs.938.70 On HDFC Bank Card 7478 At SV2512112238344230219611@ by UPI 455481482436 On 06-07 Not You? Call 18002586161/SMS BLOCK CC 7478 to 7308080808"
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(938.70, result!!.amount, 0.01)
        assertEquals("DEBIT", result.type)
    }

    @Test
    fun testHdfcAirtelRecharge() {
        val sms = "Txn Rs.911.01 On HDFC Bank Card 7478 At AIRTELPREDIRECT2@ybl by UPI 500958818056 On 25-07 Not You? Call 18002586161/SMS BLOCK CC 7478 to 7308080808"
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(911.01, result!!.amount, 0.01)
        assertEquals("Airtel", result.merchant)
        assertEquals("Bills & Utilities", result.category)
    }

    @Test
    fun testSbiUpiTransferToKrishna() {
        val sms = "Dear UPI user A/C X9477 debited by 60.00 on date 24Jul26 trf to KRISHNA KUMAR Refno 806766285828 If not u? call-1800111109 for other services-18001234-SBI"
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(60.00, result!!.amount, 0.01)
        assertEquals("KRISHNA KUMAR", result.merchant)
        assertEquals("DEBIT", result.type)
    }

    @Test
    fun testSbiUpiTransferToAvenueFoodPlaza() {
        val sms = "Dear UPI user A/C X9477 debited by 30.00 on date 24Jul26 trf to AVENUE FOOD PLAZ Refno 381425087186 If not u? call-1800111109 for other services-18001234-SBI"
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(30.00, result!!.amount, 0.01)
        assertEquals("AVENUE FOOD PLAZ", result.merchant)
        assertEquals("Food & Dining", result.category)
    }

    @Test
    fun testBobCardSpentAtithiVaibhavWithAvailableLimit() {
        val sms = "ALERT: INR 755.00 is spent on your BOBCARD ending 1158 at Upi-m S Atithi Vaibhav on 24-07-2026. Available credit limit is Rs 187,024.15, Current outstanding is Rs 44,413.22. Not you? Call 18002090 (toll-free)"
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(755.00, result!!.amount, 0.01) // Must match 755.00, NOT 187024.15
        assertEquals("S Atithi Vaibhav", result.merchant)
        assertEquals("Food & Dining", result.category)
    }

    @Test
    fun testIciciCardAmazonPayWithAvlLimit() {
        val sms = "INR 355.11 spent using ICICI Bank Card XX9000 on 22-Jul-26 on AMAZON PAY WALL. Avl Limit: INR 3,37,236.18. If not you, call 1800 2662/SMS BLOCK 9000 to 9215676766."
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(355.11, result!!.amount, 0.01) // Must match 355.11, NOT 337236.18
        assertEquals("AMAZON PAY WALL", result.merchant)
        assertEquals("Shopping", result.category)
    }

    @Test
    fun testKotakDirectAccessTradingAccount() {
        val sms = "Rs.672.00 has been debited from your Kotak Bank A/c XX8107 towards KSec trading account via Kotak Direct Access on 22-JUL-26 vide Ref No.0057787830"
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(672.00, result!!.amount, 0.01)
        assertEquals("KSec trading account", result.merchant)
        assertEquals("Investments", result.category)
    }

    @Test
    fun testSbiCreditWithAvlBal() {
        val sms = "Your A/C XXXXX819477 has credit for VREF 32431819477 674585 14JUL2 of Rs 66.00 on 16/07/26. Avl Bal Rs 70,558.91.-SBI"
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(66.00, result!!.amount, 0.01) // Must match 66.00, NOT 70558.91
        assertEquals("CREDIT", result.type)
        assertEquals("Income", result.category)
    }

    @Test
    fun testGenericAxisBankDebit() {
        val sms = "Your A/C XX1234 was debited by Rs 1,450.00 for Swiggy order on 25-07-26. Avl Bal is Rs 45,000.00."
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(1450.00, result!!.amount, 0.01)
        assertEquals("DEBIT", result.type)
        assertEquals("Swiggy", result.merchant)
        assertEquals("Food & Dining", result.category)
        assertEquals("1234", result.accountDigits)
    }

    @Test
    fun testGenericPnbCreditSalary() {
        val sms = "A/C ending 5678 credited with Rs 55,000.00 on 24-Jul-26 by NEFT salary deposit. Avl Bal Rs 82,100.00."
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(55000.00, result!!.amount, 0.01)
        assertEquals("CREDIT", result.type)
        assertEquals("Income", result.category)
        assertEquals("5678", result.accountDigits)
    }

    @Test
    fun testGenericGPayPaidNotification() {
        val notification = "Paid ₹320.00 to Uber Premier"
        val result = RegexTransactionParser.parse(notification)
        assertNotNull(result)
        assertEquals(320.00, result!!.amount, 0.01)
        assertEquals("DEBIT", result.type)
        assertEquals("Uber Premier", result.merchant)
        assertEquals("Transportation", result.category)
    }

    @Test
    fun testGenericPhonePeReceivedNotification() {
        val notification = "Received ₹500.00 from Ramesh Kumar"
        val result = RegexTransactionParser.parse(notification)
        assertNotNull(result)
        assertEquals(500.00, result!!.amount, 0.01)
        assertEquals("CREDIT", result.type)
        assertEquals("Income", result.category)
    }

    @Test
    fun testCashbackCredit() {
        val sms = "Cashback of Rs 40.00 credited to your A/c XX9000 on 26-07-26. Enjoy Paytm rewards!"
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(40.00, result!!.amount, 0.01)
        assertEquals("CREDIT", result.type)
        assertEquals("Income", result.category)
    }

    @Test
    fun testOtpMessageRejection() {
        val sms = "Your OTP is 482910 for transaction of Rs 500.00 at Amazon. Do not share code with anyone."
        val result = RegexTransactionParser.parse(sms)
        assertNull("OTP message must be rejected", result)
    }

    @Test
    fun testCreditCardBillPaymentConfirmationNotIncome() {
        val sms = "Payment of Rs.15,000.00 received towards your HDFC Bank Credit Card XX7478 on 24-JUL-26."
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(15000.00, result!!.amount, 0.01)
        assertEquals("DEBIT", result.type)
        assertEquals("Credit Card Bill", result.merchant)
        assertEquals("Bills & Utilities", result.category)
    }

    @Test
    fun testSelfTransferNotIncome() {
        val sms = "Rs 20,000.00 transferred to your A/c XX8194 from A/c XX3856"
        val result = RegexTransactionParser.parse(sms)
        assertNotNull(result)
        assertEquals(20000.00, result!!.amount, 0.01)
        assertEquals("TRANSFER", result.type)
        assertEquals("Transfer", result.category)
    }
}
