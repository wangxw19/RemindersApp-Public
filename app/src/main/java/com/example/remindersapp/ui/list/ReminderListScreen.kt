package com.example.remindersapp.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.remindersapp.data.Reminder
import com.example.remindersapp.ui.theme.RemindersAppTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// --- 这是解决问题的关键改动：使用 2025 年最新的 SwipeToDismiss API ---
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState

import androidx.compose.material3.Scaffold // 确保导入 Scaffold
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    onItemClick: (Int) -> Unit,
    onFabClick: () -> Unit,
    viewModel: ReminderListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 重新将 Scaffold 添加回来，但 TopAppBar 是全局的
    Scaffold(
        // TopAppBar 现在由 AppScaffold 管理，所以这里留空
        floatingActionButton = {
            FloatingActionButton(onClick = onFabClick) {
                Icon(Icons.Default.Add, contentDescription = "添加提醒")
            }
        }
    ) { innerPadding -> // 使用 Scaffold 提供的 padding
        ReminderList(
            reminders = uiState.reminders,
            onCheckedChange = viewModel::toggleCompleted,
            onItemClick = { reminder -> onItemClick(reminder.id) },
            onSwipeToDelete = viewModel::deleteReminder,
            modifier = Modifier.padding(innerPadding) // 应用 padding
        )
    }
}

@Composable
fun ReminderList(
    reminders: List<Reminder>,
    onCheckedChange: (Reminder) -> Unit,
    onItemClick: (Reminder) -> Unit,
    onSwipeToDelete: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reminders.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "没有提醒事项",
                style = MaterialTheme.typography.titleMedium
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(items = reminders, key = { it.id }) { reminder ->
                SwipeToDeleteContainer(
                    item = reminder,
                    onDelete = onSwipeToDelete
                ) {
                    ReminderItem(
                        reminder = it,
                        onCheckedChange = { onCheckedChange(it) },
                        onItemClick = { onItemClick(it) }
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SwipeToDeleteContainer(
    item: T,
    onDelete: (T) -> Unit,
    animationDuration: Int = 500,
    content: @Composable (T) -> Unit
) {
    var isRemoved by remember { mutableStateOf(false) }
    // 使用最新的 `rememberSwipeToDismissBoxState` API
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            // 判断滑动方向是否是我们期望的删除方向
            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                isRemoved = true
                true
            } else {
                false
            }
        },
        // 设置滑动阈值，超过一半即可触发
        positionalThreshold = { it * .5f }
    )

    LaunchedEffect(key1 = isRemoved) {
        if (isRemoved) {
            delay(animationDuration.toLong())
            onDelete(item)
        }
    }

    AnimatedVisibility(
        visible = !isRemoved,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = animationDuration),
            shrinkTowards = Alignment.Top
        ) + fadeOut()
    ) {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                DeleteBackground(swipeDismissState = dismissState)
            },
            // `content` 参数已更名为 `content`，这里保持不变，但要确保传入的是一个 Composable lambda
            content = { content(item) },
            // 仅允许从右向左滑动
            enableDismissFromStartToEnd = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteBackground(swipeDismissState: SwipeToDismissBoxState) {
    // 判断滑动方向以决定背景颜色
    val color = when (swipeDismissState.targetValue) {
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "删除",
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}


@Composable
fun ReminderItem(
    reminder: Reminder,
    onCheckedChange: () -> Unit,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onItemClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = reminder.isCompleted,
            onCheckedChange = { onCheckedChange() }
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null,
                color = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            if (!reminder.notes.isNullOrBlank()) {
                Text(
                    text = reminder.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (reminder.dueDate != null) {
                Text(
                    text = formatDateTime(reminder.dueDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

private fun formatDateTime(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}

@Preview(showBackground = true)
@Composable
fun ReminderListPreview() {
    RemindersAppTheme {
        val sampleReminders = listOf(
            Reminder(id = 1, title = "买牛奶", notes = "在楼下超市买", dueDate = null, isCompleted = false, priority = 0),
            Reminder(id = 2, title = "写代码", notes = "完成滑动删除功能", dueDate = System.currentTimeMillis(), isCompleted = true, priority = 0)
        )
        ReminderList(
            reminders = sampleReminders,
            onCheckedChange = {},
            onItemClick = {},
            onSwipeToDelete = {}
        )
    }
}