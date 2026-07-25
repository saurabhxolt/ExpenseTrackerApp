package com.expensetracker.app.ingestion.parser

data class ParsedTransaction(
    val amount: Double,
    val type: String, // DEBIT, CREDIT
    val merchant: String,
    val accountDigits: String = "",
    val category: String = "Uncategorized",
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
)
