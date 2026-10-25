package com.expensetracker.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.core.database.entity.InsurancePolicyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsurancePolicyDao {
    @Query("SELECT * FROM insurance_policies ORDER BY renewalDueDateMs ASC")
    fun getAllPolicies(): Flow<List<InsurancePolicyEntity>>

    @Query("SELECT * FROM insurance_policies ORDER BY renewalDueDateMs ASC")
    suspend fun getAllPoliciesList(): List<InsurancePolicyEntity>

    @Query("SELECT * FROM insurance_policies WHERE id = :id LIMIT 1")
    suspend fun findPolicyById(id: Long): InsurancePolicyEntity?

    @Query("SELECT * FROM insurance_policies WHERE linkedSavingsGoalId = :goalId LIMIT 1")
    suspend fun findPolicyByGoalId(goalId: Long): InsurancePolicyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicy(policy: InsurancePolicyEntity): Long

    @Update
    suspend fun updatePolicy(policy: InsurancePolicyEntity)

    @Delete
    suspend fun deletePolicy(policy: InsurancePolicyEntity)
}
