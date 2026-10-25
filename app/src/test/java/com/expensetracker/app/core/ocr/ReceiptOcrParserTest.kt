package com.expensetracker.app.core.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReceiptOcrParserTest {

    private val parser = ReceiptOcrParser()

    @Test
    fun testRestaurantBillWithTaxesAndTotal() {
        val sampleBill = """
            TAX INVOICE
            BARBEQUE NATION
            GSTIN: 29AABCB1234F1Z5
            Date: 24/10/2026 Time: 20:30
            Table No: 14
            2x Buffet Non-Veg: 1,998.00
            1x Mocktail: 199.00
            Subtotal: 2,197.00
            CGST 2.5%: 54.92
            SGST 2.5%: 54.92
            Grand Total: ₹2,416.69
            THANK YOU VISIT AGAIN
        """.trimIndent()

        val result = parser.parse(sampleBill)

        assertEquals("BARBEQUE NATION", result.merchantName)
        assertEquals(2416.69, result.amount, 0.01)
        assertEquals("Food & Dining", result.suggestedCategory)

        val cal = Calendar.getInstance()
        cal.timeInMillis = result.dateMillis
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH))
        assertEquals(24, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testPharmacyReceiptWithNetPayable() {
        val sampleBill = """
            Cash Receipt
            Apollo Pharmacy
            Store #1492
            Tel: 080-23456789
            Paracetamol 650mg: 35.00
            Azithromycin 500mg: 120.00
            Cough Syrup: 110.00
            Net Payable: Rs. 265.00
            Original for recipient
        """.trimIndent()

        val result = parser.parse(sampleBill)

        assertEquals("Apollo Pharmacy", result.merchantName)
        assertEquals(265.0, result.amount, 0.01)
        assertTrue(result.suggestedCategory.contains("Medical") || result.suggestedCategory.contains("Health") || result.suggestedCategory.contains("General"))
    }

    @Test
    fun testSupermarketGroceryReceipt() {
        val sampleBill = """
            Retail Invoice
            DMart Supermarket
            Date: 15-08-2026
            Aashirvaad Atta 5kg: 245.00
            Fortune Oil 1L: 135.00
            Tata Salt 1kg: 28.00
            TOTAL AMOUNT: 408.00
            Thank You Visit Again
        """.trimIndent()

        val result = parser.parse(sampleBill)

        assertEquals("DMart Supermarket", result.merchantName)
        assertEquals(408.0, result.amount, 0.01)
        assertTrue(result.suggestedCategory.contains("Groceries") || result.suggestedCategory.contains("Shopping") || result.suggestedCategory.contains("General"))

        val cal = Calendar.getInstance()
        cal.timeInMillis = result.dateMillis
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, cal.get(Calendar.MONTH))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }
}
