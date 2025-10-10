package com.example.remindersapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remindersapp.data.AppState
import com.example.remindersapp.data.DataBackupManager
import com.example.remindersapp.data.ReminderRepository
import com.example.remindersapp.data.ThemeSetting
import com.example.remindersapp.data.TrashReminder
import com.example.remindersapp.data.UserSettingsRepository
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
    val repository: ReminderRepository, // Changed to public to be accessed in AppScaffold
    private val userSettingsRepository: UserSettingsRepository,
    private val dataBackupManager: DataBackupManager
) : ViewModel() {

    val themeSetting = userSettingsRepository.themeSettingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemeSetting.SYSTEM
        )

    // --- Added: Expose mute status ---
    val isMuted = userSettingsRepository.isMutedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    // --- Trash-related properties ---
    val trashReminders = repository.getAllTrashRemindersStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

    // --- Added: Provide method to toggle mute status ---
    fun toggleMuteSetting() {
        viewModelScope.launch {
            val currentMuteState = isMuted.first()
            userSettingsRepository.setIsMuted(!currentMuteState)
        }
    }
    
    // Removed export method as it's handled directly in UI

    // --- Trash-related methods ---
    fun restoreFromTrash(trashReminder: TrashReminder) {
        viewModelScope.launch {
            repository.restoreFromTrash(trashReminder)
        }
    }

    fun permanentlyDeleteTrashReminder(trashReminder: TrashReminder) {
        viewModelScope.launch {
            repository.deleteTrashReminderById(trashReminder.id)
        }
    }

    fun clearTrash() {
        viewModelScope.launch {
            repository.clearTrash()
        }
    }
}