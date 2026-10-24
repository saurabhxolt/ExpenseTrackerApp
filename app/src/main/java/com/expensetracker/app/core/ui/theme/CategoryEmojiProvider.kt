package com.expensetracker.app.core.ui.theme

object CategoryEmojiProvider {

    private val EmojiMap = mapOf(
        "Food & Dining" to "🍕",
        "Food" to "🍕",
        "Dining" to "🍔",
        "Groceries & Vegetables" to "🛒",
        "Grocery" to "🛒",
        "Groceries" to "🥦",
        "Transportation" to "🚗",
        "Transport" to "🚗",
        "Travel" to "✈️",
        "Fuel" to "⛽",
        "Bills & Utilities" to "💡",
        "Bills" to "💡",
        "Utilities" to "⚡",
        "Subscriptions" to "🎬",
        "Investments" to "📈",
        "Investment" to "📈",
        "Shopping" to "🛍️",
        "Health & Medical" to "🏥",
        "Health" to "💊",
        "Insurance" to "🛡️",
        "Government & Fees" to "🏛️",
        "Rent" to "🏠",
        "EMI" to "💳",
        "Income" to "💰",
        "Incomes" to "💰",
        "Salary" to "💵",
        "Transfer" to "💸",
        "Transfers" to "🔄",
        "General Expense" to "🏷️",
        "Entertainment" to "🎮",
        "Education" to "🎓",
        "Pets" to "🐾",
        "Gifts" to "🎁",
        "Other" to "🏷️"
    )

    fun getCategoryEmoji(categoryName: String): String {
        val trimmed = categoryName.trim()
        if (trimmed.isEmpty()) return "🏷️"

        // 1. Direct match
        EmojiMap[trimmed]?.let { return it }

        // 2. Case-insensitive match
        for ((key, emoji) in EmojiMap) {
            if (key.equals(trimmed, ignoreCase = true)) {
                return emoji
            }
        }

        // 3. Keyword heuristic match
        val lower = trimmed.lowercase()
        return when {
            lower.contains("food") || lower.contains("dine") || lower.contains("eat") || lower.contains("restaurant") || lower.contains("cafe") || lower.contains("swiggy") || lower.contains("zomato") -> "🍕"
            lower.contains("groc") || lower.contains("veg") || lower.contains("supermarket") || lower.contains("blinkit") || lower.contains("zepto") -> "🛒"
            lower.contains("cab") || lower.contains("uber") || lower.contains("ola") || lower.contains("auto") || lower.contains("ride") || lower.contains("trans") -> "🚗"
            lower.contains("fuel") || lower.contains("petrol") || lower.contains("diesel") || lower.contains("gas") -> "⛽"
            lower.contains("bill") || lower.contains("util") || lower.contains("electr") || lower.contains("water") || lower.contains("airtel") || lower.contains("wifi") -> "💡"
            lower.contains("sub") || lower.contains("netflix") || lower.contains("prime") || lower.contains("spotify") -> "🎬"
            lower.contains("invest") || lower.contains("stock") || lower.contains("sip") || lower.contains("mf") || lower.contains("fund") || lower.contains("groww") || lower.contains("zerodha") -> "📈"
            lower.contains("shop") || lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("cloth") -> "🛍️"
            lower.contains("health") || lower.contains("med") || lower.contains("doc") || lower.contains("hosp") || lower.contains("pharm") || lower.contains("clinic") -> "🏥"
            lower.contains("insur") || lower.contains("policy") -> "🛡️"
            lower.contains("gov") || lower.contains("tax") || lower.contains("fine") || lower.contains("fee") || lower.contains("challan") -> "🏛️"
            lower.contains("rent") || lower.contains("house") || lower.contains("home") || lower.contains("flat") -> "🏠"
            lower.contains("emi") || lower.contains("loan") || lower.contains("credit card") || lower.contains("cred") -> "💳"
            lower.contains("incom") || lower.contains("salar") || lower.contains("bonus") || lower.contains("dividend") -> "💰"
            lower.contains("transf") || lower.contains("self") -> "💸"
            lower.contains("movi") || lower.contains("game") || lower.contains("play") -> "🎮"
            lower.contains("edu") || lower.contains("school") || lower.contains("coll") || lower.contains("book") -> "🎓"
            lower.contains("pet") || lower.contains("dog") || lower.contains("cat") -> "🐾"
            lower.contains("gift") || lower.contains("donat") -> "🎁"
            else -> "🏷️"
        }
    }
}
