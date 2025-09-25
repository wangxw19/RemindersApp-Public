package com.example.remindersapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remindersapp.data.AppState
import com.example.remindersapp.data.ReminderRepository
import com.example.remindersapp.worker.RingtonePlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val appState: AppState,
    val ringtonePlayer: RingtonePlayer,
    private val repository: ReminderRepository // 注入 Repository
) : ViewModel() {

    // --- 新增：处理应用内提醒的核心方法 ---
    fun triggerInAppRinging(reminderId: Int) {
        viewModelScope.launch {
            val reminder = repository.getReminderStream(reminderId).first()
            if (reminder != null) {
                appState.startRinging(reminder)
                ringtonePlayer.start()
            }
        }
    }
}