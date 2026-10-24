package com.expensetracker.app.features.backup

import com.expensetracker.app.core.database.entity.BudgetEntity
import com.expensetracker.app.core.database.entity.CategoryEntity
import com.expensetracker.app.core.database.entity.MerchantCategoryMappingEntity
import com.expensetracker.app.core.database.entity.SubscriptionEntity
import com.expensetracker.app.core.database.entity.TransactionEntity

data class BackupData(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val merchantMappings: List<MerchantCategoryMappingEntity> = emptyList()
)

object BackupManager {

    private const val SCHEMA_VERSION = 2

    fun createBackupJson(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        budgets: List<BudgetEntity>,
        subscriptions: List<SubscriptionEntity> = emptyList(),
        merchantMappings: List<MerchantCategoryMappingEntity> = emptyList()
    ): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"schema_version\": ").append(SCHEMA_VERSION).append(",\n")
        sb.append("  \"created_at\": ").append(System.currentTimeMillis()).append(",\n")

        // 1. Transactions
        sb.append("  \"transactions\": [\n")
        transactions.forEachIndexed { idx, t ->
            sb.append("    {\n")
            sb.append("      \"amount\": ").append(t.amount).append(",\n")
            sb.append("      \"type\": \"").append(escape(t.type)).append("\",\n")
            sb.append("      \"merchant\": \"").append(escape(t.merchant)).append("\",\n")
            sb.append("      \"rawText\": \"").append(escape(t.rawText)).append("\",\n")
            sb.append("      \"category\": \"").append(escape(t.category)).append("\",\n")
            sb.append("      \"timestamp\": ").append(t.timestamp).append(",\n")
            sb.append("      \"note\": \"").append(escape(t.note)).append("\",\n")
            sb.append("      \"accountDigits\": \"").append(escape(t.accountDigits)).append("\",\n")
            sb.append("      \"transactionHash\": \"").append(escape(t.transactionHash)).append("\",\n")
            sb.append("      \"isManual\": ").append(t.isManual).append("\n")
            sb.append("    }").append(if (idx < transactions.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // 2. Categories
        sb.append("  \"categories\": [\n")
        categories.forEachIndexed { idx, c ->
            sb.append("    {\n")
            sb.append("      \"name\": \"").append(escape(c.name)).append("\",\n")
            sb.append("      \"type\": \"").append(escape(c.type)).append("\",\n")
            sb.append("      \"iconName\": \"").append(escape(c.iconName)).append("\",\n")
            sb.append("      \"colorHex\": \"").append(escape(c.colorHex)).append("\"\n")
            sb.append("    }").append(if (idx < categories.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // 3. Budgets
        sb.append("  \"budgets\": [\n")
        budgets.forEachIndexed { idx, b ->
            sb.append("    {\n")
            sb.append("      \"categoryName\": \"").append(escape(b.categoryName)).append("\",\n")
            sb.append("      \"limitAmount\": ").append(b.limitAmount).append("\n")
            sb.append("    }").append(if (idx < budgets.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // 4. Subscriptions
        sb.append("  \"subscriptions\": [\n")
        subscriptions.forEachIndexed { idx, s ->
            sb.append("    {\n")
            sb.append("      \"name\": \"").append(escape(s.name)).append("\",\n")
            sb.append("      \"amount\": ").append(s.amount).append(",\n")
            sb.append("      \"billingCycle\": \"").append(escape(s.billingCycle)).append("\",\n")
            sb.append("      \"nextDueDateMs\": ").append(s.nextDueDateMs).append(",\n")
            sb.append("      \"category\": \"").append(escape(s.category)).append("\",\n")
            sb.append("      \"isActive\": ").append(s.isActive).append(",\n")
            sb.append("      \"isAutoDetected\": ").append(s.isAutoDetected).append(",\n")
            sb.append("      \"rawText\": \"").append(escape(s.rawText)).append("\"\n")
            sb.append("    }").append(if (idx < subscriptions.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // 5. Merchant Mappings
        sb.append("  \"merchantMappings\": [\n")
        merchantMappings.forEachIndexed { idx, m ->
            sb.append("    {\n")
            sb.append("      \"merchantKey\": \"").append(escape(m.merchantKey)).append("\",\n")
            sb.append("      \"assignedCategory\": \"").append(escape(m.assignedCategory)).append("\",\n")
            sb.append("      \"lastUpdatedMs\": ").append(m.lastUpdatedMs).append("\n")
            sb.append("    }").append(if (idx < merchantMappings.size - 1) "," else "").append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")

        return sb.toString()
    }

    fun parseBackupData(jsonStr: String): BackupData {
        if (jsonStr.isBlank()) return BackupData()

        val transactions = mutableListOf<TransactionEntity>()
        val categories = mutableListOf<CategoryEntity>()
        val budgets = mutableListOf<BudgetEntity>()
        val subscriptions = mutableListOf<SubscriptionEntity>()
        val merchantMappings = mutableListOf<MerchantCategoryMappingEntity>()

        // 1. Transactions
        val txArrayStr = extractArray(jsonStr, "transactions")
        val txObjects = splitJsonObjects(txArrayStr)
        for (obj in txObjects) {
            val merchant = extractString(obj, "merchant")
            val amount = extractDouble(obj, "amount")
            val timestamp = extractLong(obj, "timestamp").takeIf { it > 0 } ?: System.currentTimeMillis()
            val hash = extractString(obj, "transactionHash").ifBlank { "$merchant-$amount-$timestamp" }

            if (merchant.isNotBlank() || amount > 0) {
                transactions.add(
                    TransactionEntity(
                        amount = amount,
                        type = extractString(obj, "type").ifBlank { "DEBIT" },
                        merchant = merchant,
                        rawText = extractString(obj, "rawText"),
                        category = extractString(obj, "category").ifBlank { "General Expense" },
                        timestamp = timestamp,
                        note = extractString(obj, "note"),
                        accountDigits = extractString(obj, "accountDigits"),
                        transactionHash = hash,
                        isManual = extractBoolean(obj, "isManual")
                    )
                )
            }
        }

        // 2. Categories
        val catArrayStr = extractArray(jsonStr, "categories")
        val catObjects = splitJsonObjects(catArrayStr)
        for (obj in catObjects) {
            val name = extractString(obj, "name")
            if (name.isNotBlank()) {
                categories.add(
                    CategoryEntity(
                        name = name,
                        type = extractString(obj, "type").ifBlank { "EXPENSE" },
                        iconName = extractString(obj, "iconName").ifBlank { "Category" },
                        colorHex = extractString(obj, "colorHex").ifBlank { "#2563EB" }
                    )
                )
            }
        }

        // 3. Budgets
        val bgtArrayStr = extractArray(jsonStr, "budgets")
        val bgtObjects = splitJsonObjects(bgtArrayStr)
        for (obj in bgtObjects) {
            val catName = extractString(obj, "categoryName")
            val limit = extractDouble(obj, "limitAmount")
            if (catName.isNotBlank() && limit > 0) {
                budgets.add(BudgetEntity(categoryName = catName, limitAmount = limit))
            }
        }

        // 4. Subscriptions
        val subArrayStr = extractArray(jsonStr, "subscriptions")
        val subObjects = splitJsonObjects(subArrayStr)
        for (obj in subObjects) {
            val name = extractString(obj, "name")
            if (name.isNotBlank()) {
                subscriptions.add(
                    SubscriptionEntity(
                        name = name,
                        amount = extractDouble(obj, "amount"),
                        billingCycle = extractString(obj, "billingCycle").ifBlank { "Monthly" },
                        nextDueDateMs = extractLong(obj, "nextDueDateMs").takeIf { it > 0 } ?: System.currentTimeMillis(),
                        category = extractString(obj, "category").ifBlank { "Subscriptions" },
                        isActive = extractBoolean(obj, "isActive"),
                        isAutoDetected = extractBoolean(obj, "isAutoDetected"),
                        rawText = extractString(obj, "rawText")
                    )
                )
            }
        }

        // 5. Merchant Mappings
        val mapArrayStr = extractArray(jsonStr, "merchantMappings")
        val mapObjects = splitJsonObjects(mapArrayStr)
        for (obj in mapObjects) {
            val key = extractString(obj, "merchantKey")
            val assigned = extractString(obj, "assignedCategory")
            if (key.isNotBlank() && assigned.isNotBlank()) {
                merchantMappings.add(
                    MerchantCategoryMappingEntity(
                        merchantKey = key,
                        assignedCategory = assigned,
                        lastUpdatedMs = extractLong(obj, "lastUpdatedMs").takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                )
            }
        }

        return BackupData(transactions, categories, budgets, subscriptions, merchantMappings)
    }

    fun parseBackupJson(jsonStr: String): Triple<List<TransactionEntity>, List<CategoryEntity>, List<BudgetEntity>> {
        val data = parseBackupData(jsonStr)
        return Triple(data.transactions, data.categories, data.budgets)
    }

    private fun extractArray(json: String, arrayKey: String): String {
        val keyIdx = json.indexOf("\"$arrayKey\"")
        if (keyIdx == -1) return ""
        val startBracket = json.indexOf('[', keyIdx)
        if (startBracket == -1) return ""
        var depth = 0
        var inString = false
        var escape = false
        for (i in startBracket until json.length) {
            val c = json[i]
            if (escape) { escape = false; continue }
            if (c == '\\') { escape = true; continue }
            if (c == '"') { inString = !inString; continue }
            if (!inString) {
                if (c == '[') depth++
                else if (c == ']') {
                    depth--
                    if (depth == 0) {
                        return json.substring(startBracket + 1, i)
                    }
                }
            }
        }
        return ""
    }

    private fun splitJsonObjects(arrayBody: String): List<String> {
        val objects = mutableListOf<String>()
        var depth = 0
        var inString = false
        var escape = false
        var startIndex = -1

        for (i in arrayBody.indices) {
            val c = arrayBody[i]
            if (escape) {
                escape = false
                continue
            }
            if (c == '\\') {
                escape = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == '{') {
                    if (depth == 0) startIndex = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && startIndex != -1) {
                        objects.add(arrayBody.substring(startIndex + 1, i))
                        startIndex = -1
                    }
                }
            }
        }
        return objects
    }

    private fun extractString(objStr: String, key: String): String {
        val pattern = Regex("\"$key\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
        val match = pattern.find(objStr) ?: return ""
        return unescape(match.groupValues[1])
    }

    private fun extractDouble(objStr: String, key: String): Double {
        val pattern = Regex("\"$key\"\\s*:\\s*([0-9.-]+)")
        val match = pattern.find(objStr) ?: return 0.0
        return match.groupValues[1].toDoubleOrNull() ?: 0.0
    }

    private fun extractLong(objStr: String, key: String): Long {
        val pattern = Regex("\"$key\"\\s*:\\s*([0-9]+)")
        val match = pattern.find(objStr) ?: return 0L
        return match.groupValues[1].toLongOrNull() ?: 0L
    }

    private fun extractBoolean(objStr: String, key: String): Boolean {
        val pattern = Regex("\"$key\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        val match = pattern.find(objStr) ?: return false
        return match.groupValues[1].equals("true", ignoreCase = true)
    }

    private fun escape(str: String): String {
        val sb = StringBuilder()
        for (c in str) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun unescape(str: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < str.length) {
            if (str[i] == '\\' && i + 1 < str.length) {
                when (str[i + 1]) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    else -> sb.append(str[i + 1])
                }
                i += 2
            } else {
                sb.append(str[i])
                i++
            }
        }
        return sb.toString()
    }
}
