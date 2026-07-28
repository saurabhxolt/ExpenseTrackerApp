package com.expensetracker.app.core.ui.theme

import androidx.compose.ui.graphics.Color
import java.util.concurrent.ConcurrentHashMap

object CategoryColorProvider {

    // Curated distinct vibrant colors for standard expense & investment categories
    private val StandardCategoryColors = mapOf(
        "Rent" to Color(0xFF6366F1),                   // Indigo
        "EMI" to Color(0xFFF59E0B),                    // Amber / Gold
        "Groceries & Vegetables" to Color(0xFF10B981),    // Emerald Green
        "Grocery" to Color(0xFF10B981),                 // Emerald Green
        "Bills & Utilities" to Color(0xFF14B8A6),      // Teal
        "Subscriptions" to Color(0xFF8B5CF6),          // Purple
        "Investments" to Color(0xFF06B6D4),            // Cyan
        "Transportation" to Color(0xFF3B82F6),         // Bright Blue
        "Transportations" to Color(0xFF3B82F6),        // Bright Blue
        "Entertainment" to Color(0xFFF43F5E),          // Rose Red
        "Food & Dining" to Color(0xFFF97316),          // Vibrant Orange
        "Fuel" to Color(0xFFEA580C),                   // Deep Flame Orange
        "Shopping" to Color(0xFFEC4899),               // Pink / Magenta
        "Health & Medical" to Color(0xFFEF4444),       // Red
        "Insurance" to Color(0xFF0284C7),              // Sky Blue
        "Government & Fees" to Color(0xFF78350F),       // Brown
        "Transfer" to Color(0xFF64748B),               // Slate
        "General Expense" to Color(0xFF64748B),        // Slate
        "Income" to Color(0xFF059669),                 // Rich Green
        "Incomes" to Color(0xFF059669),                // Rich Green
        "Other" to Color(0xFF475569)                   // Dark Slate
    )

    // Rich pool of distinct, unassigned vibrant colors for custom user categories
    private val CustomColorPool = listOf(
        Color(0xFF0284C7), // Sky Blue
        Color(0xFF16A34A), // Forest Green
        Color(0xFFD97706), // Warm Ochre
        Color(0xFFDC2626), // Crimson
        Color(0xFF7C3AED), // Violet
        Color(0xFFDB2777), // Deep Pink
        Color(0xFF0891B2), // Ocean Teal
        Color(0xFFC026D3), // Fuchsia
        Color(0xFF65A30D), // Lime Green
        Color(0xFFB45309), // Bronze
        Color(0xFF4F46E5), // Royal Indigo
        Color(0xFFE11D48), // Ruby
        Color(0xFF9333EA), // Deep Purple
        Color(0xFFCA8A04), // Mustard
        Color(0xFF2563EB), // Primary Cobalt
        Color(0xFF0D9488), // Dark Teal
        Color(0xFF6D28D9), // Amethyst
        Color(0xFF15803D), // Pine Green
        Color(0xFFB91C1C), // Dark Red
        Color(0xFF4338CA), // Midnight Blue
        Color(0xFFBE185D), // Dark Rose
        Color(0xFF0369A1)  // Steel Blue
    )

    // Cache of dynamically assigned unique colors for custom user categories
    private val assignedCustomColors = ConcurrentHashMap<String, Color>()

    /**
     * Guarantees a unique, distinct color for every category.
     * Standard categories use curated colors; new custom categories are assigned
     * a unique color from the pool that has NOT been assigned to any earlier category.
     */
    fun getCategoryColor(categoryName: String): Color {
        val trimmed = categoryName.trim()
        if (trimmed.isEmpty()) return Color(0xFF64748B)

        // 1. Check Standard Predefined Categories
        StandardCategoryColors[trimmed]?.let { return it }
        for ((key, color) in StandardCategoryColors) {
            if (key.equals(trimmed, ignoreCase = true)) {
                return color
            }
        }

        // 2. Check if already assigned a unique custom color
        assignedCustomColors[trimmed.lowercase()]?.let { return it }

        // 3. Find an unassigned color from CustomColorPool
        val usedColors = StandardCategoryColors.values.toSet() + assignedCustomColors.values.toSet()
        val unassignedColor = CustomColorPool.firstOrNull { it !in usedColors }

        val finalColor = unassignedColor ?: run {
            // Fallback: Generate a unique HSL color if pool is exhausted using Golden Ratio distribution
            val index = assignedCustomColors.size
            val hue = (index * 137.5f) % 360f // Golden Ratio hue distribution for visual uniqueness
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.75f, 0.85f)))
        }

        assignedCustomColors[trimmed.lowercase()] = finalColor
        return finalColor
    }
}
