package com.example.remindersapp.ui.list

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.remindersapp.R
import com.example.remindersapp.data.Priority
import com.example.remindersapp.data.Reminder
import com.example.remindersapp.ui.common.EmptyContent
import com.example.remindersapp.ui.common.ReminderItem
import com.example.remindersapp.ui.common.SwipeToDeleteContainer
import com.example.remindersapp.ui.theme.RemindersAppTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onItemClick: (Int) -> Unit,
    onFabClick: () -> Unit,
    viewModel: ReminderListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onFabClick) {
                Icon(Icons.Default.Add, contentDescription = "添加提醒")
            }
        }
    ) { innerPadding ->
        ReminderList(
            reminders = uiState.reminders,
            onEvent = viewModel::onEvent,
            onItemClick = onItemClick,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderList(
    reminders: List<Reminder>,
    onEvent: (ReminderListEvent) -> Unit,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("AppDebug", "Screen: ReminderList RECOMPOSING with ${reminders.size} items")

    if (reminders.isEmpty()) {
        EmptyContent(
            title = "空空如也",
            subtitle = "点击右下角的 '+' 按钮，\n添加你的第一个提醒吧！",
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(items = reminders, key = { it.id }) { reminder ->
                SwipeToDeleteContainer(
                    modifier = Modifier.animateItemPlacement(
                        animationSpec = tween(durationMillis = 300)
                    ),
                    item = reminder,
                    onDelete = { onEvent(ReminderListEvent.OnSwipeToDelete(it)) }
                ) {
                    ReminderItem(
                        reminder = it,
                        // --- 核心修正：我们不关心 Checkbox 的新状态，直接传递 reminder 对象 ---
                        onCheckedChange = { onEvent(ReminderListEvent.OnToggleCompleted(it)) },
                        modifier = Modifier.clickable { onItemClick(it.id) }
                    )
                }
                HorizontalDivider()
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ReminderListPreview() {
    RemindersAppTheme {
        val sampleReminders = listOf(
            Reminder(id = 1, title = "买牛奶", notes = "在楼下超市买", dueDate = null, isCompleted = false, priority = Priority.HIGH),
            Reminder(id = 2, title = "写代码", notes = "完成滑动删除功能", dueDate = System.currentTimeMillis(), isCompleted = true, priority = Priority.NONE)
        )
        ReminderList(
            reminders = sampleReminders,
            onEvent = {},
            onItemClick = {}
        )
    }
}