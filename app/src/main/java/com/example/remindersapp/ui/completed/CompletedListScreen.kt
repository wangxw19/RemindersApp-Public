package com.example.remindersapp.ui.completed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.remindersapp.ui.common.EmptyContent
import com.example.remindersapp.ui.common.ReminderItem
import com.example.remindersapp.ui.common.SwipeToDeleteContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedListScreen(
    onItemClick: (Int) -> Unit,
    viewModel: CompletedListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reminders = uiState.completedReminders

    Scaffold { innerPadding ->
        if (reminders.isEmpty()) {
            EmptyContent(
                title = "还没有已完成的事项",
                subtitle = "完成一个任务后，会在这里看到它哦",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(items = reminders, key = { it.id }) { reminder ->
                    SwipeToDeleteContainer(
                        item = reminder,
                        onDelete = viewModel::deleteReminder
                    ) {
                        ReminderItem(
                            reminder = it,
                            // --- 核心修正：onCheckedChange 现在不使用 Checkbox 传出的 Boolean 值 ---
                            // 我们知道在已完成列表，点击 Checkbox 的唯一意图就是“重新激活”
                            onCheckedChange = { _ -> viewModel.reactivateReminder(it) },
                            modifier = Modifier.clickable { onItemClick(it.id) }
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}