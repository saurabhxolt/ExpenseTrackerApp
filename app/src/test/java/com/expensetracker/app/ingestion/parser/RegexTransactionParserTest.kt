package com.expensetracker.app.ingestion.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RegexTransactionParserTest {

    @Test
    fun testHdfcBankSmsDebit() {
        val sms = "Rs 450.00 debited from a/c **1234 on 25-07-26 at Swiggy. Info: VPA swiggy@icici."
        val result = RegexTransactionParser.parse(sms)

        assertNotNull(result)
        assertEquals(450.00, result!!.amount, 0.01)
        assertEquals("DEBIT", result.type)
        assertEquals("Food & Dining", result.category)
    }

    @Test
    fun testGPayUpiDebit() {
        val sms = "Sent Rs.230.00 to Uber India via VPA uber@axisbank"
        val result = RegexTransactionParser.parse(sms)

        assertNotNull(result)
        assertEquals(230.00, result!!.amount, 0.01)
        assertEquals("DEBIT", result.type)
        assertEquals("Transportation", result.category)
    }

    @Test
    fun testSalaryCredit() {
        val sms = "Rs 45000.00 credited to a/c **5678 on 25-07-26 by Salary."
        val result = RegexTransactionParser.parse(sms)

        assertNotNull(result)
        assertEquals(45000.00, result!!.amount, 0.01)
        assertEquals("CREDIT", result.type)
        assertEquals("Income", result.category)
    }
}
