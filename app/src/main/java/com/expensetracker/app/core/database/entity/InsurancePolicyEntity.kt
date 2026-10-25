package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "insurance_policies")
data class InsurancePolicyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val policyName: String,              // e.g. "Star Health Family Optima", "HDFC Life Term 1Cr", "Acko Car Comprehensive"
    val policyType: String,              // "HEALTH", "TERM", "CAR", "BIKE", "LIC", "OTHER"
    val policyNumber: String = "",       // Optional reference number
    val insurerName: String = "",        // e.g. "Star Health", "HDFC Life", "Acko", "LIC"
    val premiumAmount: Double,           // e.g. 24000.0
    val frequency: String = "Yearly",    // "Yearly", "Half-Yearly", "Quarterly"
    val renewalDueDateMs: Long,          // Next renewal deadline
    val taxSection: String = "NONE",     // "80C", "80D", "NONE"
    val linkedSavingsGoalId: Long? = null, // ID of linked Sinking Fund jar
    val isActive: Boolean = true,
    val lastPaidDateMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
