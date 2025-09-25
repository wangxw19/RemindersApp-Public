package com.example.remindersapp.ui.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remindersapp.data.Reminder
import com.example.remindersapp.data.ReminderRepository
import com.example.remindersapp.ui.AppDestinations
import com.example.remindersapp.worker.Scheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class ReminderDetailsUiState(
    val id: Int = 0,
    val title: String = "",
    val notes: String = "",
    val dueDate: Long? = null,
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
                    priority = reminder.priority,
                    isNew = false
                )
                uiState = loadedState
                // --- 核心修正：在这里赋值 ---
                initialUiState = loadedState
            }
        } else {
            initialUiState = ReminderDetailsUiState() // 新建时的初始状态
        }
    }

    // --- 这是被修复的函数 ---
    fun updateTitle(newTitle: String) {
        uiState = uiState.copy(title = newTitle)
    }

    // --- 这是被修复的函数 ---
    fun updateNotes(newNotes: String) {
        uiState = uiState.copy(notes = newNotes)
    }

    fun updateDueDate(newDate: Long?) {
        uiState = uiState.copy(dueDate = newDate)
    }

    fun saveReminder() {
        // 检查标题是否为空，如果为空则不保存或给出提示
        if (uiState.title.isBlank()) {
            // 可以在这里加一个 Toast 提示用户
            return
        }

        viewModelScope.launch {

            val reminderToSave = Reminder(
                id = if (uiState.isNew) 0 else uiState.id,
                title = uiState.title,
                notes = uiState.notes,
                dueDate = uiState.dueDate,
                isCompleted = if (uiState.isNew) false else uiState.isCompleted // 保留旧的完成状态
            )

            // --- 核心改动：输入校验 ---
            if (uiState.title.isBlank()) {
                _events.emit(DetailsScreenEvent.ShowToast("标题不能为空"))
                return@launch // 校验失败，提前返回
            }

            // WorkManager 需要一个持久化的 ID，所以我们必须先插入数据库获取 ID
            if (uiState.isNew) {
                val newId = repository.insertReminder(reminderToSave)
                // 使用返回的 ID 来更新 reminder 对象，以便调度器使用
                val savedReminder = reminderToSave.copy(id = newId.toInt())
                if (savedReminder.dueDate != null && savedReminder.dueDate > System.currentTimeMillis()) {
                    scheduler.schedule(savedReminder)
                }
            } else {
                repository.updateReminder(reminderToSave)
                if (reminderToSave.dueDate != null && reminderToSave.dueDate > System.currentTimeMillis()) {
                    scheduler.schedule(reminderToSave)
                } else {
                    scheduler.cancel(reminderToSave.id)
                }
            }

            // 成功保存后，也需要更新初始状态
            initialUiState = uiState
        }
    }

    // 添加一个获取 isCompleted 状态的方法，虽然在这个 ViewModel 中不直接用，但保持完整性
    private val ReminderDetailsUiState.isCompleted: Boolean
        get() = if (isNew) false else {
            var isCompletedState = false
            viewModelScope.launch {
                isCompletedState = repository.getReminderStream(id).filterNotNull().first().isCompleted
            }
            isCompletedState
        }
}

// --- 新增密封类，定义所有可能的 UI 事件 ---
sealed class DetailsScreenEvent {
    data class ShowToast(val message: String) : DetailsScreenEvent()
}