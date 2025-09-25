package com.example.remindersapp.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remindersapp.data.Reminder
import com.example.remindersapp.data.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// 定义 UI 状态的数据类
data class ReminderListUiState(
    val reminders: List<Reminder> = emptyList()
)

@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    val uiState: StateFlow<ReminderListUiState> =
        reminderRepository.getIncompleteRemindersStream()
            .map { reminders -> ReminderListUiState(reminders) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ReminderListUiState()
            )

    fun toggleCompleted(reminder: Reminder) {
        viewModelScope.launch {
            val updatedReminder = reminder.copy(isCompleted = !reminder.isCompleted)
            reminderRepository.updateReminder(updatedReminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(reminder)
        }
    }
}