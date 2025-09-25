package com.example.remindersapp.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remindersapp.data.Reminder
import com.example.remindersapp.data.ReminderRepository
import com.example.remindersapp.worker.Scheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ReminderListEvent {
    data class OnToggleCompleted(val reminder: Reminder) : ReminderListEvent()
    data class OnSwipeToDelete(val reminder: Reminder) : ReminderListEvent()
}

data class ReminderListUiState(
    val reminders: List<Reminder> = emptyList()
)

@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val scheduler: Scheduler
) : ViewModel() {

    val uiState: StateFlow<ReminderListUiState> =
        reminderRepository.getIncompleteRemindersStream()
            .map { ReminderListUiState(it) }
            .stateIn(
                scope = viewModelScope,
                // --- 核心修正：使用 Eagerly 策略，确保数据流永远是热的 ---
                started = SharingStarted.Eagerly,
                initialValue = ReminderListUiState()
            )

    fun onEvent(event: ReminderListEvent) {
        when (event) {
            is ReminderListEvent.OnToggleCompleted -> {
                toggleCompleted(event.reminder)
            }
            is ReminderListEvent.OnSwipeToDelete -> {
                deleteReminder(event.reminder)
            }
        }
    }

    private fun toggleCompleted(reminder: Reminder) {
        viewModelScope.launch {
            val updatedReminder = reminder.copy(isCompleted = !reminder.isCompleted)
            reminderRepository.updateReminder(updatedReminder)
            if (!updatedReminder.isCompleted && updatedReminder.dueDate != null) {
                scheduler.schedule(updatedReminder)
            } else if (updatedReminder.isCompleted) {
                scheduler.cancel(updatedReminder.id)
            }
        }
    }

    private fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            scheduler.cancel(reminder.id)
            reminderRepository.deleteReminder(reminder)
        }
    }
}