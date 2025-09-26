package com.example.remindersapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remindersapp.data.AppState
import com.example.remindersapp.data.ReminderRepository
import com.example.remindersapp.data.ThemeSetting
import com.example.remindersapp.data.ThemeSettingsRepository
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
    private val repository: ReminderRepository,
    private val themeSettingsRepository: ThemeSettingsRepository // 注入主题仓库
) : ViewModel() {

    // 将主题设置流转换为 StateFlow，以便 Compose UI 可以观察它
    val themeSetting = themeSettingsRepository.themeSettingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeSetting.SYSTEM // 提供一个初始值
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

    // 提供一个更新主题设置的方法
    fun updateThemeSetting(newSetting: ThemeSetting) {
        viewModelScope.launch {
            themeSettingsRepository.setThemeSetting(newSetting)
        }
    }
}