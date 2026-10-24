package com.expensetracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_category_mappings")
data class MerchantCategoryMappingEntity(
    @PrimaryKey
    val merchantKey: String, // Normalized lowercase merchant pattern e.g. "swiggy", "zepto", "uber"
    val assignedCategory: String, // User-selected category name e.g. "Late Night Snacks"
    val lastUpdatedMs: Long = System.currentTimeMillis()
)
