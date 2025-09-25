package com.example.remindersapp.ui.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remindersapp.data.Priority
import com.example.remindersapp.data.Reminder
import com.example.remindersapp.data.ReminderRepository
import com.example.remindersapp.ui.AppDestinations
import com.example.remindersapp.worker.Scheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

sealed class DetailsScreenEvent {
    data class ShowToast(val message: String) : DetailsScreenEvent()
}

data class ReminderDetailsUiState(
    val id: Int = 0,
    val title: String = "",
    val notes: String = "",
    val dueDate: Long? = null,
    val priority: Priority = Priority.NONE,
    val isNew: Boolean = true
)

@HiltViewModel
class ReminderDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReminderRepository,
    private val scheduler: Scheduler
) : ViewModel() {

    var uiState by mutableStateOf(ReminderDetailsUiState())
        private set

    private var initialUiState: ReminderDetailsUiState? = null

    val hasChanges: Boolean
        get() = uiState != initialUiState && initialUiState != null

    private val _events = MutableSharedFlow<DetailsScreenEvent>()
    val events = _events.asSharedFlow()

    private val itemId: Int = checkNotNull(savedStateHandle[AppDestinations.ITEM_ID_ARG])

    init {
        if (itemId != -1) {
            viewModelScope.launch {
                val reminder = repository.getReminderStream(itemId).filterNotNull().first()
                val loadedState = ReminderDetailsUiState(
                    id = reminder.id,
                    title = reminder.title,
                    notes = reminder.notes ?: "",
                    dueDate = reminder.dueDate,
                    priority = reminder.priority, // <-- 现在类型匹配了
                    isNew = false
                )
                uiState = loadedState
                initialUiState = loadedState
            }
        } else {
            initialUiState = ReminderDetailsUiState()
        }
    }

    fun updateTitle(newTitle: String) {
        uiState = uiState.copy(title = newTitle)
    }

    fun updateNotes(newNotes: String) {
        uiState = uiState.copy(notes = newNotes)
    }

    fun updateDueDate(newDate: Long?) {
        uiState = uiState.copy(dueDate = newDate)
    }

    fun setDefaultReminderTime() {
        if (uiState.dueDate == null) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            uiState = uiState.copy(dueDate = calendar.timeInMillis)
        }
    }

    fun updatePriority(newPriority: Priority) {
        uiState = uiState.copy(priority = newPriority)
    }

    fun saveReminder() {
        viewModelScope.launch {
            if (uiState.title.isBlank()) {
                _events.emit(DetailsScreenEvent.ShowToast("标题不能为空"))
                return@launch
            }

            val isCompletedState = if(uiState.isNew) false else repository.getReminderStream(uiState.id).filterNotNull().first().isCompleted

            val reminderToSave = Reminder(
                id = uiState.id,
                title = uiState.title,
                notes = uiState.notes,
                dueDate = uiState.dueDate,
                isCompleted = isCompletedState,
                priority = uiState.priority
            )

            if (uiState.isNew) {
                val newId = repository.insertReminder(reminderToSave)
                val savedReminder = reminderToSave.copy(id = newId.toInt())
                if (savedReminder.dueDate != null && savedReminder.dueDate!! > System.currentTimeMillis()) {
                    scheduler.schedule(savedReminder)
                }
            } else {
                repository.updateReminder(reminderToSave)
                if (reminderToSave.dueDate != null && reminderToSave.dueDate!! > System.currentTimeMillis()) {
                    scheduler.schedule(reminderToSave)
                } else {
                    scheduler.cancel(reminderToSave.id)
                }
            }
            initialUiState = uiState
        }
    }
}