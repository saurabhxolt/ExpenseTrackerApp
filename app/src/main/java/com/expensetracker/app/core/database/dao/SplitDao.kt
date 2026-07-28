package com.expensetracker.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expensetracker.app.core.database.entity.ExpenseSplitShareEntity
import com.expensetracker.app.core.database.entity.GroupEntity
import com.expensetracker.app.core.database.entity.GroupMemberEntity
import com.expensetracker.app.core.database.entity.GroupSettlementEntity
import com.expensetracker.app.core.database.entity.SplitExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {

    @Query("SELECT * FROM split_groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM split_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): GroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun getMembersByGroupId(groupId: Long): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getMembersListByGroupId(groupId: Long): List<GroupMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMemberEntity>)

    @Query("SELECT * FROM split_expenses WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getExpensesByGroupId(groupId: Long): Flow<List<SplitExpenseEntity>>

    @Query("SELECT * FROM split_expenses WHERE groupId = :groupId ORDER BY timestamp DESC")
    suspend fun getExpensesListByGroupId(groupId: Long): List<SplitExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: SplitExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: SplitExpenseEntity)

    @Query("SELECT * FROM expense_split_shares WHERE expenseId = :expenseId")
    suspend fun getSharesByExpenseId(expenseId: Long): List<ExpenseSplitShareEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<ExpenseSplitShareEntity>)

    @Query("SELECT * FROM group_settlements WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getSettlementsByGroupId(groupId: Long): Flow<List<GroupSettlementEntity>>

    @Query("SELECT * FROM group_settlements WHERE groupId = :groupId ORDER BY timestamp DESC")
    suspend fun getSettlementsListByGroupId(groupId: Long): List<GroupSettlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: GroupSettlementEntity): Long
}
