package com.example.remindersapp.ui.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remindersapp.data.Reminder
import com.example.remindersapp.data.ReminderRepository
import com.example.remindersapp.worker.Scheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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
        Log.d("AppDebug", "ViewModel: ReminderListViewModel INITIALIZED")
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReminderListUiState> =
        _searchQuery.flatMapLatest { query ->
            reminderRepository.searchActiveReminders(query)
        }.map { reminders ->
            Log.d("AppDebug", "ViewModel: Mapping ${reminders.size} items to UiState")
            ReminderListUiState(reminders)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReminderListUiState()
        )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

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
            reminderRepository.deleteReminder(reminder) // 现在这是软删除
        }
    }
}