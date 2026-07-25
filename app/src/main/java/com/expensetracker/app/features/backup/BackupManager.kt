package com.expensetracker.app.features.backup

import com.expensetracker.app.core.database.entity.BudgetEntity
import com.expensetracker.app.core.database.entity.CategoryEntity
import com.expensetracker.app.core.database.entity.TransactionEntity

object BackupManager {

    private const val SCHEMA_VERSION = 1

    fun createBackupJson(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        budgets: List<BudgetEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"schema_version\": ").append(SCHEMA_VERSION).append(",\n")
        sb.append("  \"created_at\": ").append(System.currentTimeMillis()).append(",\n")

        // Transactions
        sb.append("  \"transactions\": [\n")
        transactions.forEachIndexed { index, t ->
            sb.append("    {\n")
            sb.append("      \"amount\": ").append(t.amount).append(",\n")
            sb.append("      \"type\": \"").append(t.type).append("\",\n")
            sb.append("      \"merchant\": \"").append(escapeJson(t.merchant)).append("\",\n")
            sb.append("      \"rawText\": \"").append(escapeJson(t.rawText)).append("\",\n")
            sb.append("      \"category\": \"").append(escapeJson(t.category)).append("\",\n")
            sb.append("      \"timestamp\": ").append(t.timestamp).append(",\n")
            sb.append("      \"note\": \"").append(escapeJson(t.note)).append("\",\n")
            sb.append("      \"transactionHash\": \"").append(t.transactionHash).append("\",\n")
            sb.append("      \"isManual\": ").append(t.isManual).append("\n")
            sb.append("    }").append(if (index < transactions.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // Categories
        sb.append("  \"categories\": [\n")
        categories.forEachIndexed { index, c ->
            sb.append("    {\n")
            sb.append("      \"name\": \"").append(escapeJson(c.name)).append("\",\n")
            sb.append("      \"type\": \"").append(c.type).append("\",\n")
            sb.append("      \"iconName\": \"").append(c.iconName).append("\",\n")
            sb.append("      \"colorHex\": \"").append(c.colorHex).append("\"\n")
            sb.append("    }").append(if (index < categories.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // Budgets
        sb.append("  \"budgets\": [\n")
        budgets.forEachIndexed { index, b ->
            sb.append("    {\n")
            sb.append("      \"categoryName\": \"").append(escapeJson(b.categoryName)).append("\",\n")
            sb.append("      \"limitAmount\": ").append(b.limitAmount).append("\n")
            sb.append("    }").append(if (index < budgets.size - 1) "," else "").append("\n")
        }
        sb.append("  ]\n")
        sb.append("}")

        return sb.toString()
    }

    fun parseBackupJson(jsonStr: String): Triple<List<TransactionEntity>, List<CategoryEntity>, List<BudgetEntity>> {
        val transactions = mutableListOf<TransactionEntity>()
        val categories = mutableListOf<CategoryEntity>()
        val budgets = mutableListOf<BudgetEntity>()

        val lines = jsonStr.lines().map { it.trim() }
        var currentSection = ""

        var amount = 0.0
        var type = "DEBIT"
        var merchant = ""
        var rawText = ""
        var category = "General Expense"
        var timestamp = System.currentTimeMillis()
        var note = ""
        var transactionHash = ""
        var isManual = false

        var catName = ""
        var catType = "EXPENSE"
        var iconName = "Category"
        var colorHex = "#2563EB"

        var bgtCategory = ""
        var limitAmount = 0.0

        for (line in lines) {
            if (line.startsWith("\"transactions\":")) {
                currentSection = "TRANSACTIONS"
                continue
            } else if (line.startsWith("\"categories\":")) {
                currentSection = "CATEGORIES"
                continue
            } else if (line.startsWith("\"budgets\":")) {
                currentSection = "BUDGETS"
                continue
            }

            if (currentSection == "TRANSACTIONS") {
                if (line.startsWith("\"amount\":")) amount = extractDoubleValue(line)
                else if (line.startsWith("\"type\":")) type = extractStringValue(line)
                else if (line.startsWith("\"merchant\":")) merchant = extractStringValue(line)
                else if (line.startsWith("\"rawText\":")) rawText = extractStringValue(line)
                else if (line.startsWith("\"category\":")) category = extractStringValue(line)
                else if (line.startsWith("\"timestamp\":")) timestamp = extractLongValue(line)
                else if (line.startsWith("\"note\":")) note = extractStringValue(line)
                else if (line.startsWith("\"transactionHash\":")) transactionHash = extractStringValue(line)
                else if (line.startsWith("\"isManual\":")) isManual = extractBooleanValue(line)
                else if (line == "}" || line == "},") {
                    if (merchant.isNotBlank() && amount > 0) {
                        transactions.add(
                            TransactionEntity(
                                amount = amount,
                                type = type,
                                merchant = merchant,
                                rawText = rawText,
                                category = category,
                                timestamp = timestamp,
                                note = note,
                                transactionHash = if (transactionHash.isBlank()) "$merchant-$amount-$timestamp" else transactionHash,
                                isManual = isManual
                            )
                        )
                        merchant = ""
                        amount = 0.0
                    }
                }
            } else if (currentSection == "CATEGORIES") {
                if (line.startsWith("\"name\":")) catName = extractStringValue(line)
                else if (line.startsWith("\"type\":")) catType = extractStringValue(line)
                else if (line.startsWith("\"iconName\":")) iconName = extractStringValue(line)
                else if (line.startsWith("\"colorHex\":")) colorHex = extractStringValue(line)
                else if (line == "}" || line == "},") {
                    if (catName.isNotBlank()) {
                        categories.add(CategoryEntity(name = catName, type = catType, iconName = iconName, colorHex = colorHex))
                        catName = ""
                    }
                }
            } else if (currentSection == "BUDGETS") {
                if (line.startsWith("\"categoryName\":")) bgtCategory = extractStringValue(line)
                else if (line.startsWith("\"limitAmount\":")) limitAmount = extractDoubleValue(line)
                else if (line == "}" || line == "},") {
                    if (bgtCategory.isNotBlank() && limitAmount > 0) {
                        budgets.add(BudgetEntity(categoryName = bgtCategory, limitAmount = limitAmount))
                        bgtCategory = ""
                        limitAmount = 0.0
                    }
                }
            }
        }

        return Triple(transactions, categories, budgets)
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    }

    private fun extractStringValue(line: String): String {
        val colonIdx = line.indexOf(":")
        if (colonIdx == -1) return ""
        val valuePart = line.substring(colonIdx + 1).trim()
        val firstQuote = valuePart.indexOf("\"")
        val lastQuote = valuePart.lastIndexOf("\"")
        if (firstQuote != -1 && lastQuote > firstQuote) {
            return valuePart.substring(firstQuote + 1, lastQuote).replace("\\\"", "\"").replace("\\\\", "\\")
        }
        return ""
    }

    private fun extractDoubleValue(line: String): Double {
        val valStr = line.replace(Regex("[^0-9.]"), "")
        return valStr.toDoubleOrNull() ?: 0.0
    }

    private fun extractLongValue(line: String): Long {
        val valStr = line.replace(Regex("[^0-9]"), "")
        return valStr.toLongOrNull() ?: 0L
    }

    private fun extractBooleanValue(line: String): Boolean {
        return line.contains("true", ignoreCase = true)
    }
}
