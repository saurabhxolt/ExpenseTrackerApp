package com.expensetracker.app.core.promotions

data class Promotion(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String = "",
    val actionUrl: String = "",
    val priority: Int = 1,
    val startDate: String = "",
    val endDate: String = "",
    val minimumAppVersion: Int = 1,
    val enabled: Boolean = true
)

data class Announcement(
    val id: String,
    val title: String,
    val description: String,
    val actionUrl: String = "",
    val enabled: Boolean = true
)

data class PromotionConfig(
    val version: Int = 1,
    val refreshIntervalHours: Int = 24,
    val promotions: List<Promotion> = emptyList(),
    val announcements: List<Announcement> = emptyList()
)
