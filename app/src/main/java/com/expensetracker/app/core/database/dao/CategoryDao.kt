package com.expensetracker.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expensetracker.app.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET name = :newName, type = :newType, emoji = :newEmoji, colorHex = :newColorHex WHERE id = :id")
    suspend fun updateCategoryInPlace(id: Long, newName: String, newType: String, newEmoji: String, newColorHex: String)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
}
