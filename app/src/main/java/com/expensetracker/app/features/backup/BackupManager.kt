package com.expensetracker.app.features.backup

import com.expensetracker.app.core.database.entity.BudgetEntity
import com.expensetracker.app.core.database.entity.CategoryEntity
import com.expensetracker.app.core.database.entity.ExpenseSplitShareEntity
import com.expensetracker.app.core.database.entity.GroupEntity
import com.expensetracker.app.core.database.entity.GroupMemberEntity
import com.expensetracker.app.core.database.entity.GroupSettlementEntity
import com.expensetracker.app.core.database.entity.MerchantCategoryMappingEntity
import com.expensetracker.app.core.database.entity.SavingsGoalEntity
import com.expensetracker.app.core.database.entity.CreditCardEntity
import com.expensetracker.app.core.database.entity.SplitExpenseEntity
import com.expensetracker.app.core.database.entity.SubscriptionEntity
import com.expensetracker.app.core.database.entity.TransactionEntity

data class BackupData(
    val transactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val merchantMappings: List<MerchantCategoryMappingEntity> = emptyList(),
    val groups: List<GroupEntity> = emptyList(),
    val groupMembers: List<GroupMemberEntity> = emptyList(),
    val splitExpenses: List<SplitExpenseEntity> = emptyList(),
    val splitShares: List<ExpenseSplitShareEntity> = emptyList(),
    val groupSettlements: List<GroupSettlementEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val creditCards: List<CreditCardEntity> = emptyList()
)

object BackupManager {

    private const val SCHEMA_VERSION = 3

    fun createBackupJson(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        budgets: List<BudgetEntity>,
        subscriptions: List<SubscriptionEntity> = emptyList(),
        merchantMappings: List<MerchantCategoryMappingEntity> = emptyList(),
        groups: List<GroupEntity> = emptyList(),
        groupMembers: List<GroupMemberEntity> = emptyList(),
        splitExpenses: List<SplitExpenseEntity> = emptyList(),
        splitShares: List<ExpenseSplitShareEntity> = emptyList(),
        groupSettlements: List<GroupSettlementEntity> = emptyList(),
        savingsGoals: List<SavingsGoalEntity> = emptyList(),
        creditCards: List<CreditCardEntity> = emptyList()
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
            sb.append("      \"isManual\": ").append(t.isManual).append(",\n")
            sb.append("      \"taxDeductibleSection\": \"").append(escape(t.taxDeductibleSection ?: "")).append("\"\n")
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
            sb.append("      \"colorHex\": \"").append(escape(c.colorHex)).append("\",\n")
            sb.append("      \"isCustom\": ").append(c.isCustom).append(",\n")
            sb.append("      \"emoji\": \"").append(escape(c.emoji)).append("\"\n")
            sb.append("    }").append(if (idx < categories.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // 3. Budgets
        sb.append("  \"budgets\": [\n")
        budgets.forEachIndexed { idx, b ->
            sb.append("    {\n")
            sb.append("      \"categoryName\": \"").append(escape(b.categoryName)).append("\",\n")
            sb.append("      \"limitAmount\": ").append(b.limitAmount).append(",\n")
            sb.append("      \"period\": \"").append(escape(b.period)).append("\"\n")
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
            sb.append("      \"rawText\": \"").append(escape(s.rawText)).append("\",\n")
            sb.append("      \"autoMatched\": ").append(s.autoMatched).append(",\n")
            sb.append("      \"lastPaidDateMs\": ").append(s.lastPaidDateMs ?: 0L).append("\n")
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
        sb.append("  ],\n")

        // 6. Split Groups
        sb.append("  \"groups\": [\n")
        groups.forEachIndexed { idx, g ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(g.id).append(",\n")
            sb.append("      \"name\": \"").append(escape(g.name)).append("\",\n")
            sb.append("      \"description\": \"").append(escape(g.description)).append("\",\n")
            sb.append("      \"emoji\": \"").append(escape(g.emoji)).append("\",\n")
            sb.append("      \"createdAt\": ").append(g.createdAt).append("\n")
            sb.append("    }").append(if (idx < groups.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // 7. Group Members
        sb.append("  \"groupMembers\": [\n")
        groupMembers.forEachIndexed { idx, m ->
            sb.append("    {\n")
            sb.append("      \"groupId\": ").append(m.groupId).append(",\n")
            sb.append("      \"memberName\": \"").append(escape(m.memberName)).append("\"\n")
            sb.append("    }").append(if (idx < groupMembers.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // 8. Split Expenses
        sb.append("  \"splitExpenses\": [\n")
        splitExpenses.forEachIndexed { idx, e ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(e.id).append(",\n")
            sb.append("      \"groupId\": ").append(e.groupId).append(",\n")
            sb.append("      \"title\": \"").append(escape(e.title)).append("\",\n")
            sb.append("      \"totalAmount\": ").append(e.totalAmount).append(",\n")
            sb.append("      \"paidByMember\": \"").append(escape(e.paidByMember)).append("\",\n")
            sb.append("      \"category\": \"").append(escape(e.category)).append("\",\n")
            sb.append("      \"timestamp\": ").append(e.timestamp).append("\n")
            sb.append("    }").append(if (idx < splitExpenses.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // 9. Split Shares
        sb.append("  \"splitShares\": [\n")
        splitShares.forEachIndexed { idx, s ->
            sb.append("    {\n")
            sb.append("      \"expenseId\": ").append(s.expenseId).append(",\n")
            sb.append("      \"memberName\": \"").append(escape(s.memberName)).append("\",\n")
            sb.append("      \"shareAmount\": ").append(s.shareAmount).append("\n")
            sb.append("    }").append(if (idx < splitShares.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // 10. Group Settlements
        sb.append("  \"groupSettlements\": [\n")
        groupSettlements.forEachIndexed { idx, s ->
            sb.append("    {\n")
            sb.append("      \"groupId\": ").append(s.groupId).append(",\n")
            sb.append("      \"fromMember\": \"").append(escape(s.fromMember)).append("\",\n")
            sb.append("      \"toMember\": \"").append(escape(s.toMember)).append("\",\n")
            sb.append("      \"amount\": ").append(s.amount).append(",\n")
            sb.append("      \"timestamp\": ").append(s.timestamp).append("\n")
            sb.append("    }").append(if (idx < groupSettlements.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // 11. Savings Goals
        sb.append("  \"savingsGoals\": [\n")
        savingsGoals.forEachIndexed { idx, g ->
            sb.append("    {\n")
            sb.append("      \"title\": \"").append(escape(g.title)).append("\",\n")
            sb.append("      \"targetAmount\": ").append(g.targetAmount).append(",\n")
            sb.append("      \"currentAmount\": ").append(g.currentAmount).append(",\n")
            sb.append("      \"targetDate\": ").append(g.targetDate).append(",\n")
            sb.append("      \"colorHex\": \"").append(escape(g.colorHex)).append("\",\n")
            sb.append("      \"emoji\": \"").append(escape(g.emoji)).append("\",\n")
            sb.append("      \"note\": \"").append(escape(g.note)).append("\",\n")
            sb.append("      \"isCompleted\": ").append(g.isCompleted).append(",\n")
            sb.append("      \"createdAt\": ").append(g.createdAt).append("\n")
            sb.append("    }").append(if (idx < savingsGoals.size - 1) "," else "").append("\n")
        }
        sb.append("  ],\n")

        // 12. Credit Cards
        sb.append("  \"creditCards\": [\n")
        creditCards.forEachIndexed { idx, c ->
            sb.append("    {\n")
            sb.append("      \"cardName\": \"").append(escape(c.cardName)).append("\",\n")
            sb.append("      \"last4Digits\": \"").append(escape(c.last4Digits)).append("\",\n")
            sb.append("      \"totalLimit\": ").append(c.totalLimit).append(",\n")
            sb.append("      \"statementDayOfMonth\": ").append(c.statementDayOfMonth).append(",\n")
            sb.append("      \"dueDayOfMonth\": ").append(c.dueDayOfMonth).append(",\n")
            sb.append("      \"cardColorHex\": ").append(c.cardColorHex).append(",\n")
            sb.append("      \"bankName\": \"").append(escape(c.bankName)).append("\",\n")
            sb.append("      \"isActive\": ").append(c.isActive).append(",\n")
            sb.append("      \"createdAt\": ").append(c.createdAt).append("\n")
            sb.append("    }").append(if (idx < creditCards.size - 1) "," else "").append("\n")
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
        val groups = mutableListOf<GroupEntity>()
        val groupMembers = mutableListOf<GroupMemberEntity>()
        val splitExpenses = mutableListOf<SplitExpenseEntity>()
        val splitShares = mutableListOf<ExpenseSplitShareEntity>()
        val groupSettlements = mutableListOf<GroupSettlementEntity>()

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
                        isManual = extractBoolean(obj, "isManual"),
                        taxDeductibleSection = extractString(obj, "taxDeductibleSection").ifBlank { null }
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
                        colorHex = extractString(obj, "colorHex").ifBlank { "#2563EB" },
                        isCustom = extractBoolean(obj, "isCustom"),
                        emoji = extractString(obj, "emoji")
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
            val period = extractString(obj, "period").ifBlank { "MONTHLY" }
            if (catName.isNotBlank() && limit > 0) {
                budgets.add(BudgetEntity(categoryName = catName, limitAmount = limit, period = period))
            }
        }

        // 4. Subscriptions
        val subArrayStr = extractArray(jsonStr, "subscriptions")
        val subObjects = splitJsonObjects(subArrayStr)
        for (obj in subObjects) {
            val name = extractString(obj, "name")
            if (name.isNotBlank()) {
                val lastPaidMs = extractLong(obj, "lastPaidDateMs").takeIf { it > 0 }
                subscriptions.add(
                    SubscriptionEntity(
                        name = name,
                        amount = extractDouble(obj, "amount"),
                        billingCycle = extractString(obj, "billingCycle").ifBlank { "Monthly" },
                        nextDueDateMs = extractLong(obj, "nextDueDateMs").takeIf { it > 0 } ?: System.currentTimeMillis(),
                        category = extractString(obj, "category").ifBlank { "Subscriptions" },
                        isActive = extractBoolean(obj, "isActive"),
                        isAutoDetected = extractBoolean(obj, "isAutoDetected"),
                        rawText = extractString(obj, "rawText"),
                        autoMatched = extractBoolean(obj, "autoMatched"),
                        lastPaidDateMs = lastPaidMs
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

        // 6. Split Groups
        val grpArrayStr = extractArray(jsonStr, "groups")
        val grpObjects = splitJsonObjects(grpArrayStr)
        for (obj in grpObjects) {
            val name = extractString(obj, "name")
            if (name.isNotBlank()) {
                groups.add(
                    GroupEntity(
                        id = extractLong(obj, "id"),
                        name = name,
                        description = extractString(obj, "description"),
                        emoji = extractString(obj, "emoji").ifBlank { "✈️" },
                        createdAt = extractLong(obj, "createdAt").takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                )
            }
        }

        // 7. Group Members
        val memArrayStr = extractArray(jsonStr, "groupMembers")
        val memObjects = splitJsonObjects(memArrayStr)
        for (obj in memObjects) {
            val memberName = extractString(obj, "memberName").ifBlank { extractString(obj, "name") }
            val groupId = extractLong(obj, "groupId")
            if (memberName.isNotBlank() && groupId > 0) {
                groupMembers.add(
                    GroupMemberEntity(
                        groupId = groupId,
                        memberName = memberName
                    )
                )
            }
        }

        // 8. Split Expenses
        val expArrayStr = extractArray(jsonStr, "splitExpenses")
        val expObjects = splitJsonObjects(expArrayStr)
        for (obj in expObjects) {
            val title = extractString(obj, "title")
            val groupId = extractLong(obj, "groupId")
            if (title.isNotBlank() && groupId > 0) {
                splitExpenses.add(
                    SplitExpenseEntity(
                        id = extractLong(obj, "id"),
                        groupId = groupId,
                        title = title,
                        totalAmount = extractDouble(obj, "totalAmount"),
                        paidByMember = extractString(obj, "paidByMember").ifBlank { extractString(obj, "paidByMemberName") },
                        category = extractString(obj, "category").ifBlank { "General Expense" },
                        timestamp = extractLong(obj, "timestamp").takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                )
            }
        }

        // 9. Split Shares
        val shareArrayStr = extractArray(jsonStr, "splitShares")
        val shareObjects = splitJsonObjects(shareArrayStr)
        for (obj in shareObjects) {
            val expenseId = extractLong(obj, "expenseId")
            val memberName = extractString(obj, "memberName")
            if (expenseId > 0 && memberName.isNotBlank()) {
                splitShares.add(
                    ExpenseSplitShareEntity(
                        expenseId = expenseId,
                        memberName = memberName,
                        shareAmount = extractDouble(obj, "shareAmount")
                    )
                )
            }
        }

        // 10. Group Settlements
        val setArrayStr = extractArray(jsonStr, "groupSettlements")
        val setObjects = splitJsonObjects(setArrayStr)
        for (obj in setObjects) {
            val groupId = extractLong(obj, "groupId")
            val from = extractString(obj, "fromMember").ifBlank { extractString(obj, "fromMemberName") }
            val to = extractString(obj, "toMember").ifBlank { extractString(obj, "toMemberName") }
            if (groupId > 0 && from.isNotBlank() && to.isNotBlank()) {
                groupSettlements.add(
                    GroupSettlementEntity(
                        groupId = groupId,
                        fromMember = from,
                        toMember = to,
                        amount = extractDouble(obj, "amount"),
                        timestamp = extractLong(obj, "timestamp").takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                )
            }
        }

        // 11. Savings Goals
        val savingsGoals = mutableListOf<SavingsGoalEntity>()
        val goalArrayStr = extractArray(jsonStr, "savingsGoals")
        val goalObjects = splitJsonObjects(goalArrayStr)
        for (obj in goalObjects) {
            val title = extractString(obj, "title")
            val targetAmount = extractDouble(obj, "targetAmount")
            if (title.isNotBlank() && targetAmount > 0) {
                savingsGoals.add(
                    SavingsGoalEntity(
                        title = title,
                        targetAmount = targetAmount,
                        currentAmount = extractDouble(obj, "currentAmount"),
                        targetDate = extractLong(obj, "targetDate"),
                        colorHex = extractString(obj, "colorHex").ifBlank { "#10B981" },
                        emoji = extractString(obj, "emoji").ifBlank { "🎯" },
                        note = extractString(obj, "note"),
                        isCompleted = extractBoolean(obj, "isCompleted"),
                        createdAt = extractLong(obj, "createdAt").takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                )
            }
        }

        // 12. Credit Cards
        val creditCards = mutableListOf<CreditCardEntity>()
        val cardArrayStr = extractArray(jsonStr, "creditCards")
        val cardObjects = splitJsonObjects(cardArrayStr)
        for (obj in cardObjects) {
            val cardName = extractString(obj, "cardName")
            val digits = extractString(obj, "last4Digits")
            val limit = extractDouble(obj, "totalLimit")
            if (cardName.isNotBlank() || digits.isNotBlank()) {
                creditCards.add(
                    CreditCardEntity(
                        cardName = cardName.ifBlank { "Card" },
                        last4Digits = digits,
                        totalLimit = if (limit > 0) limit else 50000.0,
                        statementDayOfMonth = extractLong(obj, "statementDayOfMonth").toInt().coerceIn(1, 31).takeIf { it > 0 } ?: 15,
                        dueDayOfMonth = extractLong(obj, "dueDayOfMonth").toInt().coerceIn(1, 31).takeIf { it > 0 } ?: 5,
                        cardColorHex = extractLong(obj, "cardColorHex").takeIf { it != 0L } ?: 0xFF1E3A8A,
                        bankName = extractString(obj, "bankName").ifBlank { "Bank" },
                        isActive = extractBoolean(obj, "isActive"),
                        createdAt = extractLong(obj, "createdAt").takeIf { it > 0 } ?: System.currentTimeMillis()
                    )
                )
            }
        }

        return BackupData(transactions, categories, budgets, subscriptions, merchantMappings, groups, groupMembers, splitExpenses, splitShares, groupSettlements, savingsGoals, creditCards)
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
