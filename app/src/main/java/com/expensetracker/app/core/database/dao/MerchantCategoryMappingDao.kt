package com.expensetracker.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expensetracker.app.core.database.entity.MerchantCategoryMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantCategoryMappingDao {

    @Query("SELECT * FROM merchant_category_mappings")
    fun getAllMappings(): Flow<List<MerchantCategoryMappingEntity>>

    @Query("SELECT * FROM merchant_category_mappings")
    suspend fun getAllMappingsList(): List<MerchantCategoryMappingEntity>

    @Query("SELECT * FROM merchant_category_mappings WHERE merchantKey = :merchantKey LIMIT 1")
    suspend fun getMappingForMerchant(merchantKey: String): MerchantCategoryMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: MerchantCategoryMappingEntity)

    @Query("DELETE FROM merchant_category_mappings WHERE merchantKey = :merchantKey")
    suspend fun deleteMapping(merchantKey: String)
}
