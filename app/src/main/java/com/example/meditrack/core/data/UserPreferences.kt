package com.example.meditrack.core.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val name: String = "",
    val role: String = "GENERAL",
    val onboardingComplete: Boolean = false
)

class UserPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("meditrack_preferences", Context.MODE_PRIVATE)
    private val _profile = MutableStateFlow(readProfile())
    private val _units = MutableStateFlow(readUnits())
    private val _theme = MutableStateFlow(preferences.getString(KEY_THEME, "PINK") ?: "PINK")

    val profile: StateFlow<UserProfile> = _profile.asStateFlow()
    val units: StateFlow<List<String>> = _units.asStateFlow()
    val theme: StateFlow<String> = _theme.asStateFlow()

    fun completeOnboarding(name: String, role: String) {
        preferences.edit()
            .putString(KEY_NAME, name.trim())
            .putString(KEY_ROLE, role)
            .putBoolean(KEY_ONBOARDING, true)
            .apply()
        _profile.value = readProfile()
    }

    fun addUnit(unit: String) {
        val normalized = unit.trim().lowercase()
        if (normalized.isBlank()) return
        val updated = (_units.value + normalized).distinct().sorted()
        preferences.edit().putString(KEY_UNITS, updated.joinToString("|")).apply()
        _units.value = updated
    }

    fun updateName(name: String) {
        if (name.isBlank()) return
        preferences.edit().putString(KEY_NAME, name.trim()).apply()
        _profile.value = readProfile()
    }

    fun updateTheme(theme: String) {
        preferences.edit().putString(KEY_THEME, theme).apply()
        _theme.value = theme
    }

    private fun readProfile() = UserProfile(
        name = preferences.getString(KEY_NAME, "").orEmpty(),
        role = preferences.getString(KEY_ROLE, "GENERAL") ?: "GENERAL",
        onboardingComplete = preferences.getBoolean(KEY_ONBOARDING, false)
    )

    private fun readUnits(): List<String> {
        val saved = preferences.getString(KEY_UNITS, null)
        return saved?.split("|")?.filter(String::isNotBlank)
            ?: listOf("mg", "g", "gr", "ml", "mcg", "ui")
    }

    companion object {
        private const val KEY_NAME = "user_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_ONBOARDING = "onboarding_complete"
        private const val KEY_UNITS = "custom_units"
        private const val KEY_THEME = "app_theme"
    }
}
