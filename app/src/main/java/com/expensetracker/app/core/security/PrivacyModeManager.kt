package com.expensetracker.app.core.security

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

val LocalPrivacyMode = compositionLocalOf { false }

@Singleton
class PrivacyModeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isPrivacyMode = MutableStateFlow(prefs.getBoolean(KEY_DEFAULT_PRIVACY_MODE, false))
    val isPrivacyMode: StateFlow<Boolean> = _isPrivacyMode.asStateFlow()

    fun toggle() {
        _isPrivacyMode.value = !_isPrivacyMode.value
    }

    fun set(enabled: Boolean) {
        _isPrivacyMode.value = enabled
    }

    fun isShakeToHideEnabled(): Boolean {
        return prefs.getBoolean(KEY_SHAKE_TO_HIDE, true)
    }

    fun setShakeToHideEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHAKE_TO_HIDE, enabled).apply()
    }

    fun isDefaultPrivacyModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_DEFAULT_PRIVACY_MODE, false)
    }

    fun setDefaultPrivacyModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEFAULT_PRIVACY_MODE, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "expense_tracker_prefs"
        private const val KEY_SHAKE_TO_HIDE = "privacy_shake_to_hide_enabled"
        private const val KEY_DEFAULT_PRIVACY_MODE = "privacy_default_mode_enabled"
    }
}
