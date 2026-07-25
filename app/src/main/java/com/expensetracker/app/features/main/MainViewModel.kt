package com.expensetracker.app.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.promotions.Promotion
import com.expensetracker.app.core.promotions.PromotionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val promotionManager: PromotionManager
) : ViewModel() {

    private val _promotions = MutableStateFlow<List<Promotion>>(emptyList())
    val promotions: StateFlow<List<Promotion>> = _promotions

    init {
        loadPromotions()
    }

    fun loadPromotions() {
        viewModelScope.launch {
            _promotions.value = promotionManager.getActivePromotions()
        }
    }
}
