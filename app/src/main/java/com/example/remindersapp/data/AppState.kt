package com.example.remindersapp.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppState {
    private val _currentRingingReminder = MutableStateFlow<Reminder?>(null)
    val currentRingingReminder = _currentRingingReminder.asStateFlow()

    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground = _isAppInForeground.asStateFlow()

    fun startRinging(reminder: Reminder) {
        _currentRingingReminder.value = reminder
    }

    fun stopRinging() {
        _currentRingingReminder.value = null
    }

    fun setAppInForeground(isInForeground: Boolean) {
        _isAppInForeground.value = isInForeground
    }
}