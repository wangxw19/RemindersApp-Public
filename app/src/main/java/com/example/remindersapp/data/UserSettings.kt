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

// 主题选项保持不变
enum class ThemeSetting {
    SYSTEM, LIGHT, DARK
}

// 通过属性委托创建 DataStore 实例，名称保持为 "settings"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserSettingsRepository @Inject constructor( // 类名重命名
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val THEME_SETTING = stringPreferencesKey("theme_setting")
        // 新增：静音设置的 Key
        val IS_MUTED = booleanPreferencesKey("is_muted")
    }

    // --- 主题设置部分保持不变 ---
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

    // --- 新增：静音设置部分 ---
    val isMutedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_MUTED] ?: false // 默认不静音
        }

    suspend fun setIsMuted(isMuted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_MUTED] = isMuted
        }
    }
}