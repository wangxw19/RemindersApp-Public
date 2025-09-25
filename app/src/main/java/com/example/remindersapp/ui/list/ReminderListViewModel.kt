package com.example.remindersapp.ui.list

import android.util.Log
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

    init {
        // --- 哨兵 V1: 确认 ViewModel 已被 Hilt 成功创建 ---
        Log.d("AppDebug", "ViewModel: ReminderListViewModel INITIALIZED")
    }

    val uiState: StateFlow<ReminderListUiState> =
        reminderRepository.getIncompleteRemindersStream()
            .map { reminders ->
                // --- 哨兵 V2: 确认 ViewModel 正在处理从 Repository 过来的数据 ---
                Log.d("AppDebug", "ViewModel: Mapping ${reminders.size} items to UiState")
                ReminderListUiState(reminders)
            }
            .stateIn(
                scope = viewModelScope,
                // 我们暂时使用 Eagerly 策略来解决问题，日志会告诉我们这是否是根源
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