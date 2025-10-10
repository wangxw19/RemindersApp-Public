package com.example.remindersapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Theme options remain unchanged
enum class ThemeSetting {
    SYSTEM, LIGHT, DARK
}

// Create DataStore instance via property delegation, name remains "settings"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserSettingsRepository @Inject constructor( // Class name renamed
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val THEME_SETTING = stringPreferencesKey("theme_setting")
        // Added: Key for mute setting
        val IS_MUTED = booleanPreferencesKey("is_muted")
    }

    // --- Theme setting part remains unchanged ---
    val themeSettingFlow: Flow<ThemeSetting> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_SETTING] ?: ThemeSetting.SYSTEM.name
            try {
                ThemeSetting.valueOf(themeName)
            } catch (e: IllegalArgumentException) {
                ThemeSetting.SYSTEM
            }
        }

    suspend fun setThemeSetting(themeSetting: ThemeSetting) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_SETTING] = themeSetting.name
        }
    }

    // --- Added: Mute setting part ---
    val isMutedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_MUTED] ?: false // Default is unmuted
        }

    suspend fun setIsMuted(isMuted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_MUTED] = isMuted
        }
    }
}