package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryName: String,
    val limitAmount: Double,
    val period: String = "MONTHLY" // MONTHLY, YEARLY
)
