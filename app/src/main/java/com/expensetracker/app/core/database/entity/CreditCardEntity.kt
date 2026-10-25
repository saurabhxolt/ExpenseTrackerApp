package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardName: String,               // e.g. "HDFC Millennia", "ICICI Amazon Pay", "SBI SimplyCLICK"
    val last4Digits: String,            // e.g. "1234"
    val totalLimit: Double,             // e.g. 100000.0
    val statementDayOfMonth: Int = 15,  // Day statement generates (1..31)
    val dueDayOfMonth: Int = 5,         // Payment due day of following cycle (1..31)
    val cardColorHex: Long = 0xFF1E3A8A,// Navy blue default
    val bankName: String = "Bank",      // HDFC, SBI, ICICI, Axis, Kotak, Amex, etc.
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
