package com.expensetracker.app.features.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.database.dao.CategoryDao
import com.expensetracker.app.core.database.entity.CategoryEntity
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
    private val categoryDao: CategoryDao
) : ViewModel() {

    val uiState: StateFlow<CategoriesUiState> = categoryDao.getAllCategories().map { list ->
        CategoriesUiState(categories = list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState()
    )

    fun addCategory(name: String, type: String, iconName: String, colorHex: String) {
        viewModelScope.launch {
            val entity = CategoryEntity(
                name = name,
                type = type,
                iconName = iconName,
                colorHex = colorHex
            )
            categoryDao.insertCategories(listOf(entity))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
        }
    }
}
