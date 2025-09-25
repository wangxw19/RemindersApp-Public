package com.example.remindersapp.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.example.remindersapp.ui.theme.RemindersAppTheme
import kotlinx.coroutines.delay
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
            onItemClick = { reminder -> onItemClick(reminder.id) },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// --- 核心改动 1：为 ReminderList 添加 @OptIn(ExperimentalFoundationApi::class) ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderList(
    reminders: List<Reminder>,
    onEvent: (ReminderListEvent) -> Unit,
    onItemClick: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    // --- 核心改动 2：将 animateItemPlacement 修饰符传递给列表项的根容器 ---
                    modifier = Modifier.animateItemPlacement(
                        animationSpec = tween(durationMillis = 300)
                    ),
                    item = reminder,
                    onDelete = { onEvent(ReminderListEvent.OnSwipeToDelete(it)) }
                ) {
                    ReminderItem(
                        reminder = it,
                        onCheckedChange = { onEvent(ReminderListEvent.OnToggleCompleted(it)) },
                        onItemClick = { onItemClick(it) }
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun EmptyContent(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_empty_state_illustration),
            contentDescription = "空列表插图",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SwipeToDeleteContainer(
    modifier: Modifier = Modifier, // 接收 animateItemPlacement
    item: T,
    onDelete: (T) -> Unit,
    animationDuration: Int = 500,
    content: @Composable (T) -> Unit
) {
    var isRemoved by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                isRemoved = true
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * .5f }
    )

    LaunchedEffect(key1 = isRemoved) {
        if (isRemoved) {
            delay(animationDuration.toLong())
            onDelete(item)
        }
    }

    AnimatedVisibility(
        modifier = modifier, // 将修饰符应用到这里
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
            content = { content(item) },
            enableDismissFromStartToEnd = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteBackground(swipeDismissState: SwipeToDismissBoxState) {
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
    // --- 核心改动 3：为背景和文本颜色添加动画状态 ---
    val targetBackgroundColor by animateColorAsState(
        targetValue = if (reminder.isCompleted) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 300),
        label = "ItemBackgroundColor"
    )

    val targetContentColor by animateColorAsState(
        targetValue = if (reminder.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 300),
        label = "ItemContentColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(targetBackgroundColor) // 应用动画背景色
            .clickable(onClick = onItemClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(4.dp, 36.dp)) {
            drawRect(color = reminder.priority.color)
        }
        Spacer(modifier = Modifier.width(8.dp))

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
                color = targetContentColor // 应用动画文本颜色
            )
            if (!reminder.notes.isNullOrBlank()) {
                Text(
                    text = reminder.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // 备注颜色保持不变
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

@Preview(showBackground = true)
@Composable
fun EmptyContentPreview() {
    RemindersAppTheme {
        EmptyContent(title = "Title", subtitle = "Subtitle")
    }
}