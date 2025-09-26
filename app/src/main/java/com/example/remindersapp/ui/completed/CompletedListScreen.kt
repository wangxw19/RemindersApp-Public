package com.example.remindersapp.ui.completed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.remindersapp.ui.list.EmptyContent
import com.example.remindersapp.ui.list.ReminderItem
import com.example.remindersapp.ui.list.SwipeToDeleteContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletedListScreen(
    onItemClick: (Int) -> Unit,
    viewModel: CompletedListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val reminders = uiState.completedReminders

    // 这个 Screen 没有任何 FAB，所以可以直接是 LazyColumn
    if (reminders.isEmpty()) {
        EmptyContent(
            title = "还没有已完成的事项",
            subtitle = "完成一个任务后，会在这里看到它哦"
        )
    } else {
        LazyColumn {
            items(items = reminders, key = { it.id }) { reminder ->
                SwipeToDeleteContainer(
                    item = reminder,
                    onDelete = viewModel::deleteReminder
                ) {
                    ReminderItem(
                        reminder = it,
                        onCheckedChange = { viewModel.reactivateReminder(it) },
                        onItemClick = { onItemClick(it.id) }
                    )
                }
                HorizontalDivider()
            }
        }
    }
}