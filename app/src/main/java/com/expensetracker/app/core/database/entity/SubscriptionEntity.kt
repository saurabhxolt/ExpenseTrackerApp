package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val billingCycle: String = "Monthly", // Monthly, Yearly, Weekly, Quarterly
    val nextDueDateMs: Long = System.currentTimeMillis(),
    val category: String = "Subscriptions",
    val isActive: Boolean = true,
    val isAutoDetected: Boolean = false,
    val rawText: String = "",
    val lastPaidTransactionId: Long? = null,
    val autoMatched: Boolean = false,
    val lastPaidDateMs: Long? = null
)
