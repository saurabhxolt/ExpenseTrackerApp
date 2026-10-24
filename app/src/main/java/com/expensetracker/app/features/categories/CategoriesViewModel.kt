package com.expensetracker.app.features.categories

import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.database.dao.CategoryDao
import com.expensetracker.app.core.database.dao.TransactionDao
import com.expensetracker.app.core.database.entity.CategoryEntity
import com.expensetracker.app.core.ui.theme.CategoryColorProvider
import com.expensetracker.app.core.ui.theme.CategoryEmojiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<CategoryEntity> = emptyList()
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : ViewModel() {

    companion object {
        val DefaultSystemCategories = listOf(
            CategoryEntity(id = 0, name = "Food & Dining", type = "EXPENSE", iconName = "Category", colorHex = "#F97316", isCustom = false, emoji = "🍕"),
            CategoryEntity(id = 0, name = "Groceries & Vegetables", type = "EXPENSE", iconName = "Category", colorHex = "#10B981", isCustom = false, emoji = "🛒"),
            CategoryEntity(id = 0, name = "Transportation", type = "EXPENSE", iconName = "Category", colorHex = "#3B82F6", isCustom = false, emoji = "🚗"),
            CategoryEntity(id = 0, name = "Fuel", type = "EXPENSE", iconName = "Category", colorHex = "#EA580C", isCustom = false, emoji = "⛽"),
            CategoryEntity(id = 0, name = "Bills & Utilities", type = "EXPENSE", iconName = "Category", colorHex = "#14B8A6", isCustom = false, emoji = "💡"),
            CategoryEntity(id = 0, name = "Subscriptions", type = "EXPENSE", iconName = "Category", colorHex = "#8B5CF6", isCustom = false, emoji = "🎬"),
            CategoryEntity(id = 0, name = "Investments", type = "INVESTMENT", iconName = "Category", colorHex = "#06B6D4", isCustom = false, emoji = "📈"),
            CategoryEntity(id = 0, name = "Shopping", type = "EXPENSE", iconName = "Category", colorHex = "#EC4899", isCustom = false, emoji = "🛍️"),
            CategoryEntity(id = 0, name = "Health & Medical", type = "EXPENSE", iconName = "Category", colorHex = "#EF4444", isCustom = false, emoji = "🏥"),
            CategoryEntity(id = 0, name = "Insurance", type = "EXPENSE", iconName = "Category", colorHex = "#0284C7", isCustom = false, emoji = "🛡️"),
            CategoryEntity(id = 0, name = "Government & Fees", type = "EXPENSE", iconName = "Category", colorHex = "#78350F", isCustom = false, emoji = "🏛️"),
            CategoryEntity(id = 0, name = "Rent", type = "EXPENSE", iconName = "Category", colorHex = "#6366F1", isCustom = false, emoji = "🏠"),
            CategoryEntity(id = 0, name = "EMI", type = "EXPENSE", iconName = "Category", colorHex = "#F59E0B", isCustom = false, emoji = "💳"),
            CategoryEntity(id = 0, name = "Income", type = "INCOME", iconName = "Category", colorHex = "#059669", isCustom = false, emoji = "💰"),
            CategoryEntity(id = 0, name = "Transfer", type = "TRANSFER", iconName = "Category", colorHex = "#64748B", isCustom = false, emoji = "💸"),
            CategoryEntity(id = 0, name = "Entertainment", type = "EXPENSE", iconName = "Category", colorHex = "#F43F5E", isCustom = false, emoji = "🎮"),
            CategoryEntity(id = 0, name = "General Expense", type = "EXPENSE", iconName = "Category", colorHex = "#64748B", isCustom = false, emoji = "🏷️")
        )
    }

    init {
        viewModelScope.launch {
            if (categoryDao.getCategoryCount() == 0) {
                categoryDao.insertCategories(DefaultSystemCategories)
            }
        }
    }

    val uiState: StateFlow<CategoriesUiState> = categoryDao.getAllCategories().map { customList ->
        val fullList = if (customList.isEmpty()) {
            DefaultSystemCategories
        } else {
            customList
        }.map { cat ->
            val color = CategoryColorProvider.getCategoryColor(cat.name)
            val hex = String.format("#%06X", 0xFFFFFF and color.toArgb())
            val emoji = if (cat.emoji.isNotBlank()) cat.emoji else CategoryEmojiProvider.getCategoryEmoji(cat.name)
            cat.copy(colorHex = hex, emoji = emoji)
        }.sortedBy { it.name }

        CategoriesUiState(categories = fullList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState()
    )

    fun addCategory(name: String, type: String, iconName: String = "Category", userEmoji: String = "") {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            val color = CategoryColorProvider.getCategoryColor(trimmedName)
            val hex = String.format("#%06X", 0xFFFFFF and color.toArgb())
            val emoji = if (userEmoji.isNotBlank()) userEmoji else CategoryEmojiProvider.getCategoryEmoji(trimmedName)

            val entity = CategoryEntity(
                name = trimmedName,
                type = type,
                iconName = iconName,
                colorHex = hex,
                isCustom = true,
                emoji = emoji
            )
            categoryDao.insertCategories(listOf(entity))
        }
    }

    fun updateCategory(category: CategoryEntity, newName: String, newType: String, newEmoji: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) return
        viewModelScope.launch {
            val color = CategoryColorProvider.getCategoryColor(trimmedName)
            val hex = String.format("#%06X", 0xFFFFFF and color.toArgb())
            val emoji = if (newEmoji.isNotBlank()) newEmoji else CategoryEmojiProvider.getCategoryEmoji(trimmedName)

            if (category.id > 0) {
                // Perform true in-place SQL UPDATE
                categoryDao.updateCategoryInPlace(
                    id = category.id,
                    newName = trimmedName,
                    newType = newType,
                    newEmoji = emoji,
                    newColorHex = hex
                )
            } else {
                val entity = CategoryEntity(
                    id = 0,
                    name = trimmedName,
                    type = newType,
                    iconName = category.iconName,
                    colorHex = hex,
                    isCustom = category.isCustom,
                    emoji = emoji
                )
                categoryDao.insertCategories(listOf(entity))
            }

            // Sync historical transactions if category name changed
            if (category.name.lowercase() != trimmedName.lowercase()) {
                transactionDao.updateTransactionCategoryName(category.name, trimmedName)
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (category.id > 0) {
                categoryDao.deleteCategory(category)
            }
        }
    }
}
