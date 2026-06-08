package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "manavahana_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        private val SECURITY_PIN = stringPreferencesKey("security_pin")
        private val IS_PIN_LOCK_ENABLED = booleanPreferencesKey("is_pin_lock_enabled")
        private val IS_FINGERPRINT_ENABLED = booleanPreferencesKey("is_fingerprint_enabled")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val JWT_TOKEN = stringPreferencesKey("jwt_token")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_MODE] ?: "system"
    }

    val jwtToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[JWT_TOKEN]
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ONBOARDING_COMPLETED] ?: false
    }

    val securityPin: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SECURITY_PIN]
    }

    val isPinLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_PIN_LOCK_ENABLED] ?: (preferences[SECURITY_PIN] != null)
    }

    val isFingerprintEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_FINGERPRINT_ENABLED] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun savePin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[SECURITY_PIN] = pin
            preferences[IS_PIN_LOCK_ENABLED] = true
        }
    }

    suspend fun clearPin() {
        context.dataStore.edit { preferences ->
            preferences.remove(SECURITY_PIN)
            preferences[IS_PIN_LOCK_ENABLED] = false
        }
    }

    suspend fun setFingerprintEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FINGERPRINT_ENABLED] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    suspend fun saveJwt(token: String) {
        context.dataStore.edit { preferences ->
            preferences[JWT_TOKEN] = token
        }
    }

    suspend fun clearJwt() {
        context.dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN)
        }
    }
}
