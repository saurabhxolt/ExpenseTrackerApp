package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["transactionHash"], unique = true)]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: String, // DEBIT, CREDIT, TRANSFER
    val merchant: String,
    val rawText: String,
    val category: String,
    val timestamp: Long,
    val note: String = "",
    val accountDigits: String = "",
    val transactionHash: String,
    val isDuplicate: Boolean = false,
    val isManual: Boolean = false
)
