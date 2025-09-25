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

// --- 模块化交互的基石：定义 UI 事件 ---
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
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = ReminderListUiState()
            )

    // --- 统一的事件处理入口 ---
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
            // 如果任务重新激活并有截止日期，则重新调度
            if (!updatedReminder.isCompleted && updatedReminder.dueDate != null) {
                scheduler.schedule(updatedReminder)
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