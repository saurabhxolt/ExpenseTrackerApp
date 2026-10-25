package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_incomes")
data class RecurringIncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // e.g. "TechCorp Salary", "Flat 402 Rent", "LIC Annuity"
    val amount: Double,                  // e.g. 85000.0
    val frequency: String = "Monthly",   // "Monthly", "Quarterly", "Semi-Annually", "Yearly", "Bi-Weekly"
    val expectedDayOfMonth: Int = 1,     // 1..31
    val nextDueDateMs: Long = System.currentTimeMillis(),
    val category: String = "Salary",     // "Salary", "Rental Income", "Investments", "Business", "Pensions / Annuity", "General Income"
    val settlementMode: String = "SMS_AUTO_MATCH", // "SMS_AUTO_MATCH", "PROMPT_ON_DUE", "AUTO_POST_CASH"
    val isActive: Boolean = true,
    val isSalary: Boolean = false,       // Flags primary salary for Salary Day Provisioning
    val lastReceivedDateMs: Long? = null,
    val matchedTransactionId: Long? = null,
    val payerPpiOrBank: String = "",     // e.g. "INFOSYS", "HDFC", "TCS"
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
