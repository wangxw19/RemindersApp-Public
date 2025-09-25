package com.example.remindersapp.ui.completed

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

data class CompletedListUiState(
    val completedReminders: List<Reminder> = emptyList()
)

@HiltViewModel
class CompletedListViewModel @Inject constructor(
    private val repository: ReminderRepository
) : ViewModel() {

    val uiState: StateFlow<CompletedListUiState> =
        repository.getCompletedRemindersStream()
            .map { CompletedListUiState(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L), // 修正拼写
                initialValue = CompletedListUiState()
            )

    fun reactivateReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isCompleted = false))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }
}