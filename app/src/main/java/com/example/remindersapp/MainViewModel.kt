package com.example.remindersapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remindersapp.data.AppState
import com.example.remindersapp.data.ReminderRepository
import com.example.remindersapp.data.ThemeSetting
import com.example.remindersapp.data.UserSettingsRepository
import com.example.remindersapp.utils.DataExportImportManager
import com.example.remindersapp.worker.RingtonePlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val appState: AppState,
    val ringtonePlayer: RingtonePlayer,
    val repository: ReminderRepository, // 改为public以在AppScaffold中访问
    private val userSettingsRepository: UserSettingsRepository,
    private val dataExportImportManager: DataExportImportManager
) : ViewModel() {

    val themeSetting = userSettingsRepository.themeSettingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeSetting.SYSTEM
        )

    // --- 新增：暴露静音状态 ---
    val isMuted = userSettingsRepository.isMutedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    fun triggerInAppRinging(reminderId: Int) {
        viewModelScope.launch {
            val reminder = repository.getReminderStream(reminderId).first()
            if (reminder != null) {
                appState.startRinging(reminder)
                ringtonePlayer.start()
            }
        }
    }

    fun updateThemeSetting(newSetting: ThemeSetting) {
        viewModelScope.launch {
            userSettingsRepository.setThemeSetting(newSetting)
        }
    }

    // --- 新增：提供切换静音状态的方法 ---
    fun toggleMuteSetting() {
        viewModelScope.launch {
            val currentMuteState = isMuted.first()
            userSettingsRepository.setIsMuted(!currentMuteState)
        }
    }
    
    // 移除导出方法，因为已在UI中直接处理
    
    // --- 新增：导入数据方法 ---
    suspend fun importData(uri: android.net.Uri): Boolean {
        return dataExportImportManager.importData(uri)
    }
}