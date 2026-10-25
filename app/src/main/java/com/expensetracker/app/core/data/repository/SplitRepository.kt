package com.expensetracker.app.core.data.repository

import com.expensetracker.app.core.database.dao.SplitDao
import com.expensetracker.app.core.database.entity.ExpenseSplitShareEntity
import com.expensetracker.app.core.database.entity.GroupEntity
import com.expensetracker.app.core.database.entity.GroupMemberEntity
import com.expensetracker.app.core.database.entity.GroupSettlementEntity
import com.expensetracker.app.core.database.entity.SplitExpenseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min

data class MemberBalance(
    val memberName: String,
    val netBalance: Double
)

data class DebtTransfer(
    val fromMember: String,
    val toMember: String,
    val amount: Double
)

data class GroupDetailSummary(
    val group: GroupEntity,
    val members: List<String>,
    val totalSpend: Double,
    val userNetBalance: Double,
    val memberBalances: List<MemberBalance>,
    val debtTransfers: List<DebtTransfer>
)

@Singleton
class SplitRepository @Inject constructor(
    private val splitDao: SplitDao
) {
    val allGroups: Flow<List<GroupEntity>> = splitDao.getAllGroups()

    fun getMembersByGroup(groupId: Long): Flow<List<GroupMemberEntity>> =
        splitDao.getMembersByGroupId(groupId)

    fun getExpensesByGroup(groupId: Long): Flow<List<SplitExpenseEntity>> =
        splitDao.getExpensesByGroupId(groupId)

    fun getSettlementsByGroup(groupId: Long): Flow<List<GroupSettlementEntity>> =
        splitDao.getSettlementsByGroupId(groupId)

    suspend fun createGroup(name: String, description: String, emoji: String, memberNames: List<String>): Long {
        val group = GroupEntity(
            name = name.trim(),
            description = description.trim(),
            emoji = emoji.ifBlank { "✈️" }
        )
        val groupId = splitDao.insertGroup(group)

        // Ensure "You" is included as default member if not present
        val cleanMembers = (listOf("You") + memberNames.map { it.trim() })
            .filter { it.isNotBlank() }
            .distinct()

        val memberEntities = cleanMembers.map {
            GroupMemberEntity(groupId = groupId, memberName = it)
        }
        splitDao.insertMembers(memberEntities)

        return groupId
    }

    suspend fun deleteGroup(group: GroupEntity) {
        splitDao.deleteGroup(group)
    }

    suspend fun addExpense(
        groupId: Long,
        title: String,
        totalAmount: Double,
        paidByMember: String,
        category: String,
        customShares: Map<String, Double>? = null
    ): Long {
        val expense = SplitExpenseEntity(
            groupId = groupId,
            title = title.trim(),
            totalAmount = totalAmount,
            paidByMember = paidByMember.trim(),
            category = category
        )
        val expenseId = splitDao.insertExpense(expense)

        val members = splitDao.getMembersListByGroupId(groupId).map { it.memberName }
        val sharesList = mutableListOf<ExpenseSplitShareEntity>()

        if (customShares != null && customShares.isNotEmpty()) {
            customShares.forEach { (memberName, share) ->
                if (share > 0) {
                    sharesList.add(ExpenseSplitShareEntity(expenseId = expenseId, memberName = memberName, shareAmount = share))
                }
            }
        } else {
            // Equal split among all group members
            val count = if (members.isNotEmpty()) members.size else 1
            val equalShare = totalAmount / count
            members.forEach { memberName ->
                sharesList.add(ExpenseSplitShareEntity(expenseId = expenseId, memberName = memberName, shareAmount = equalShare))
            }
        }

        splitDao.insertShares(sharesList)
        return expenseId
    }

    suspend fun deleteExpense(expense: SplitExpenseEntity) {
        splitDao.deleteExpense(expense)
    }

    suspend fun recordSettlement(groupId: Long, fromMember: String, toMember: String, amount: Double) {
        if (amount > 0 && fromMember != toMember) {
            val settlement = GroupSettlementEntity(
                groupId = groupId,
                fromMember = fromMember.trim(),
                toMember = toMember.trim(),
                amount = amount
            )
            splitDao.insertSettlement(settlement)
        }
    }

    suspend fun getGroupSummary(groupId: Long): GroupDetailSummary? {
        val group = splitDao.getGroupById(groupId) ?: return null
        val members = splitDao.getMembersListByGroupId(groupId).map { it.memberName }
        val expenses = splitDao.getExpensesListByGroupId(groupId)
        val settlements = splitDao.getSettlementsListByGroupId(groupId)

        val balanceMap = members.associateWith { 0.0 }.toMutableMap()
        var totalGroupSpend = 0.0

        for (exp in expenses) {
            totalGroupSpend += exp.totalAmount

            // Paid member gets + totalAmount
            balanceMap[exp.paidByMember] = (balanceMap[exp.paidByMember] ?: 0.0) + exp.totalAmount

            // Members with shares get - shareAmount
            val shares = splitDao.getSharesByExpenseId(exp.id)
            for (share in shares) {
                balanceMap[share.memberName] = (balanceMap[share.memberName] ?: 0.0) - share.shareAmount
            }
        }

        for (settlement in settlements) {
            balanceMap[settlement.fromMember] = (balanceMap[settlement.fromMember] ?: 0.0) + settlement.amount
            balanceMap[settlement.toMember] = (balanceMap[settlement.toMember] ?: 0.0) - settlement.amount
        }

        val memberBalances = balanceMap.map { MemberBalance(it.key, it.value) }
            .sortedByDescending { it.netBalance }

        val debtTransfers = calculateSimplifiedDebts(balanceMap)

        val userNetBalance = balanceMap["You"] ?: 0.0

        return GroupDetailSummary(
            group = group,
            members = members,
            totalSpend = totalGroupSpend,
            userNetBalance = userNetBalance,
            memberBalances = memberBalances,
            debtTransfers = debtTransfers
        )
    }

    fun generateWhatsAppSummaryText(summary: GroupDetailSummary): String {
        val sb = StringBuilder()
        sb.append("${summary.group.emoji} *${summary.group.name} - Settlement Summary* ${summary.group.emoji}\n")
        if (summary.group.description.isNotBlank()) {
            sb.append("_${summary.group.description}_\n")
        }
        sb.append("Total Group Spend: ₹${String.format("%.2f", summary.totalSpend)}\n\n")

        sb.append("*Net Balances:*\n")
        for (mb in summary.memberBalances) {
            val sign = if (mb.netBalance >= 0) "+" else ""
            sb.append("• ${mb.memberName}: $sign₹${String.format("%.2f", mb.netBalance)}\n")
        }

        sb.append("\n*Who Owes Whom:*\n")
        if (summary.debtTransfers.isEmpty()) {
            sb.append("✅ All group expenses are fully settled!\n")
        } else {
            for (dt in summary.debtTransfers) {
                sb.append("👉 *${dt.fromMember}* owes *${dt.toMember}* ₹${String.format("%.2f", dt.amount)}\n")
            }
        }

        sb.append("\n_Generated via Expense Tracker App (100% Offline & Private)_")
        return sb.toString()
    }

    private fun calculateSimplifiedDebts(balances: Map<String, Double>): List<DebtTransfer> {
        val debtors = mutableListOf<Pair<String, Double>>()
        val creditors = mutableListOf<Pair<String, Double>>()

        for ((member, bal) in balances) {
            if (bal < -0.01) debtors.add(member to abs(bal))
            else if (bal > 0.01) creditors.add(member to bal)
        }

        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }

        val transfers = mutableListOf<DebtTransfer>()
        var dIdx = 0
        var cIdx = 0

        val dList = debtors.map { Pair(it.first, it.second) }.toMutableList()
        val cList = creditors.map { Pair(it.first, it.second) }.toMutableList()

        while (dIdx < dList.size && cIdx < cList.size) {
            val debtor = dList[dIdx]
            val creditor = cList[cIdx]

            val amount = min(debtor.second, creditor.second)
            if (amount > 0.01) {
                transfers.add(DebtTransfer(fromMember = debtor.first, toMember = creditor.first, amount = amount))
            }

            dList[dIdx] = debtor.first to (debtor.second - amount)
            cList[cIdx] = creditor.first to (creditor.second - amount)

            if (dList[dIdx].second <= 0.01) dIdx++
            if (cList[cIdx].second <= 0.01) cIdx++
        }

        return transfers
    }

    suspend fun exportGroupSyncPayload(groupId: Long, senderName: String): com.expensetracker.app.core.sync.GroupSyncPayload? {
        val group = splitDao.getGroupById(groupId) ?: return null
        val members = splitDao.getMembersListByGroupId(groupId).map { it.memberName }
        val expenses = splitDao.getExpensesListByGroupId(groupId)
        val settlements = splitDao.getSettlementsListByGroupId(groupId)

        val expSyncList = expenses.map { exp ->
            val shares = splitDao.getSharesByExpenseId(exp.id).map {
                com.expensetracker.app.core.sync.ShareSyncData(it.memberName, it.shareAmount)
            }
            com.expensetracker.app.core.sync.ExpenseSyncData(
                title = exp.title,
                totalAmount = exp.totalAmount,
                paidByMember = exp.paidByMember,
                category = exp.category,
                timestamp = exp.timestamp,
                shares = shares
            )
        }

        val settSyncList = settlements.map { sett ->
            com.expensetracker.app.core.sync.SettlementSyncData(
                fromMember = sett.fromMember,
                toMember = sett.toMember,
                amount = sett.amount,
                timestamp = sett.timestamp
            )
        }

        return com.expensetracker.app.core.sync.GroupSyncPayload(
            version = 1,
            senderName = senderName.trim().ifBlank { "Friend" },
            group = com.expensetracker.app.core.sync.GroupSyncData(
                name = group.name,
                description = group.description,
                emoji = group.emoji,
                createdAt = group.createdAt,
                members = members,
                expenses = expSyncList,
                settlements = settSyncList
            )
        )
    }

    suspend fun mergeGroupSyncData(
        groupData: com.expensetracker.app.core.sync.GroupSyncData,
        targetGroupId: Long? = null
    ): Long {
        val finalGroupId: Long
        if (targetGroupId == null) {
            val group = GroupEntity(
                name = groupData.name.trim(),
                description = groupData.description.trim(),
                emoji = groupData.emoji.ifBlank { "✈️" },
                createdAt = groupData.createdAt
            )
            finalGroupId = splitDao.insertGroup(group)

            val cleanMembers = (listOf("You") + groupData.members.map { it.trim() })
                .filter { it.isNotBlank() }
                .distinct()

            val memberEntities = cleanMembers.map {
                GroupMemberEntity(groupId = finalGroupId, memberName = it)
            }
            splitDao.insertMembers(memberEntities)
        } else {
            finalGroupId = targetGroupId
            val existingMembers = splitDao.getMembersListByGroupId(finalGroupId).map { it.memberName }.toSet()
            val missingMembers = groupData.members
                .map { it.trim() }
                .filter { it.isNotBlank() && !existingMembers.contains(it) }
                .distinct()
            if (missingMembers.isNotEmpty()) {
                val newEntities = missingMembers.map {
                    GroupMemberEntity(groupId = finalGroupId, memberName = it)
                }
                splitDao.insertMembers(newEntities)
            }
        }

        // Merge Expenses (idempotent matching on title, amount, timestamp)
        val existingExpenses = splitDao.getExpensesListByGroupId(finalGroupId)
        for (inExp in groupData.expenses) {
            val duplicate = existingExpenses.any {
                it.title.equals(inExp.title.trim(), ignoreCase = true) &&
                    abs(it.totalAmount - inExp.totalAmount) < 0.01 &&
                    abs(it.timestamp - inExp.timestamp) < 5000L // 5-second window
            }
            if (!duplicate) {
                val newExp = SplitExpenseEntity(
                    groupId = finalGroupId,
                    title = inExp.title.trim(),
                    totalAmount = inExp.totalAmount,
                    paidByMember = inExp.paidByMember.trim(),
                    category = inExp.category,
                    timestamp = inExp.timestamp
                )
                val expenseId = splitDao.insertExpense(newExp)

                val shares = inExp.shares.map {
                    ExpenseSplitShareEntity(
                        expenseId = expenseId,
                        memberName = it.memberName.trim(),
                        shareAmount = it.shareAmount
                    )
                }
                if (shares.isNotEmpty()) {
                    splitDao.insertShares(shares)
                }
            }
        }

        // Merge Settlements (idempotent matching on from, to, amount, timestamp)
        val existingSettlements = splitDao.getSettlementsListByGroupId(finalGroupId)
        for (inSett in groupData.settlements) {
            val duplicate = existingSettlements.any {
                it.fromMember.equals(inSett.fromMember.trim(), ignoreCase = true) &&
                    it.toMember.equals(inSett.toMember.trim(), ignoreCase = true) &&
                    abs(it.amount - inSett.amount) < 0.01 &&
                    abs(it.timestamp - inSett.timestamp) < 5000L
            }
            if (!duplicate && inSett.amount > 0) {
                val newSett = GroupSettlementEntity(
                    groupId = finalGroupId,
                    fromMember = inSett.fromMember.trim(),
                    toMember = inSett.toMember.trim(),
                    amount = inSett.amount,
                    timestamp = inSett.timestamp
                )
                splitDao.insertSettlement(newSett)
            }
        }

        return finalGroupId
    }
}
