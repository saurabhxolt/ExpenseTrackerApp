package com.expensetracker.app.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.promotions.Promotion
import com.expensetracker.app.core.promotions.PromotionManager
import com.expensetracker.app.core.security.PrivacyModeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val promotionManager: PromotionManager,
    private val privacyModeManager: PrivacyModeManager
) : ViewModel() {

    private val _promotions = MutableStateFlow<List<Promotion>>(emptyList())
    val promotions: StateFlow<List<Promotion>> = _promotions

    val isPrivacyMode: StateFlow<Boolean> = privacyModeManager.isPrivacyMode

    init {
        loadPromotions()
    }

    fun loadPromotions() {
        viewModelScope.launch {
            _promotions.value = promotionManager.getActivePromotions()
        }
    }

    fun togglePrivacyMode() {
        privacyModeManager.toggle()
    }
}
