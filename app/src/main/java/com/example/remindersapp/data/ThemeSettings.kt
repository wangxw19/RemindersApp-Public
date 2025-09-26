package com.example.remindersapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// 定义主题选项
enum class ThemeSetting {
    SYSTEM, LIGHT, DARK
}

// 通过属性委托创建 DataStore 实例
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class ThemeSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val THEME_SETTING = stringPreferencesKey("theme_setting")
    }

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
}