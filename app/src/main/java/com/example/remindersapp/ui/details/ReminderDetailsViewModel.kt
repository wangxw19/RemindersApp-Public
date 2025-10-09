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
    object NavigateUp : DetailsScreenEvent()
}

sealed class DetailsUIEvent {
    data class OnTitleChange(val title: String) : DetailsUIEvent()
    data class OnNotesChange(val notes: String) : DetailsUIEvent()
    data class OnPriorityChange(val priority: Priority) : DetailsUIEvent()
    object OnSetDefaultTime : DetailsUIEvent()
    data class OnDateSelected(val dateMillis: Long?) : DetailsUIEvent()
    data class OnTimeSelected(val hour: Int, val minute: Int) : DetailsUIEvent()
    object OnClearDate : DetailsUIEvent()
    object OnSaveClick : DetailsUIEvent()
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
    private val savedStateHandle: SavedStateHandle,
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
                    priority = reminder.priority,
                    isNew = false
                )
                uiState = loadedState
                initialUiState = loadedState
            }
        } else {
            initialUiState = ReminderDetailsUiState()
        }
    }

    fun onEvent(event: DetailsUIEvent) {
        when (event) {
            is DetailsUIEvent.OnTitleChange -> uiState = uiState.copy(title = event.title)
            is DetailsUIEvent.OnNotesChange -> uiState = uiState.copy(notes = event.notes)
            is DetailsUIEvent.OnPriorityChange -> uiState = uiState.copy(priority = event.priority)
            is DetailsUIEvent.OnSetDefaultTime -> setDefaultReminderTime()
            is DetailsUIEvent.OnDateSelected -> updateDate(event.dateMillis)
            is DetailsUIEvent.OnTimeSelected -> updateTime(event.hour, event.minute)
            is DetailsUIEvent.OnClearDate -> uiState = uiState.copy(dueDate = null)
            is DetailsUIEvent.OnSaveClick -> saveReminder()
        }
    }

    private fun setDefaultReminderTime() {
        if (uiState.dueDate == null) {
            val calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
            uiState = uiState.copy(dueDate = calendar.timeInMillis)
        }
    }

    private fun updateDate(selectedDateMillis: Long?) {
        if (selectedDateMillis == null) return
        val newDate = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val currentDueDate = Calendar.getInstance().apply {
            timeInMillis = uiState.dueDate ?: System.currentTimeMillis()
        }
        currentDueDate.set(
            newDate.get(Calendar.YEAR),
            newDate.get(Calendar.MONTH),
            newDate.get(Calendar.DAY_OF_MONTH)
        )
        uiState = uiState.copy(dueDate = currentDueDate.timeInMillis)
    }

    private fun updateTime(hour: Int, minute: Int) {
        val currentDueDate = Calendar.getInstance().apply {
            timeInMillis = uiState.dueDate ?: System.currentTimeMillis()
        }
        currentDueDate.set(Calendar.HOUR_OF_DAY, hour)
        currentDueDate.set(Calendar.MINUTE, minute)
        currentDueDate.set(Calendar.SECOND, 0)
        uiState = uiState.copy(dueDate = currentDueDate.timeInMillis)
    }

    private fun saveReminder() {
        viewModelScope.launch {
            if (uiState.title.isBlank()) {
                _events.emit(DetailsScreenEvent.ShowToast("标题不能为空"))
                return@launch
            }

            val isCompletedState = if (uiState.isNew) false else repository.getReminderStream(uiState.id).filterNotNull().first().isCompleted

            val reminderToSave = Reminder(
                id = uiState.id,
                title = uiState.title,
                notes = uiState.notes,
                dueDate = uiState.dueDate,
                isCompleted = isCompletedState,
                isDeleted = false, // 新建或编辑的提醒默认不是删除状态
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
                    // 如果更新后的任务没有日期或日期已过，取消现有的调度
                    scheduler.cancel(reminderToSave.id)
                }
            }
            initialUiState = uiState
            _events.emit(DetailsScreenEvent.NavigateUp)
        }
    }
}