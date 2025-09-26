package com.example.remindersapp.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.remindersapp.R
import com.example.remindersapp.data.Reminder
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

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
    modifier: Modifier = Modifier,
    item: T,
    onDelete: (T) -> Unit,
    content: @Composable (T) -> Unit
) {
    var isRemoved by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                isRemoved = true
                true
            } else {
                false
            }
        }
    )

    LaunchedEffect(key1 = isRemoved) {
        if (isRemoved) {
            delay(500)
            onDelete(item)
        }
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = !isRemoved,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 500),
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ReminderItem(
    reminder: Reminder,
    // --- 核心修正 1：将参数类型改为接收一个 Boolean ---
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardElevation by animateDpAsState(
        targetValue = if (reminder.isCompleted) 1.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "CardElevation"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        shadowElevation = cardElevation,
        tonalElevation = cardElevation
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(4.dp, 36.dp)) {
                drawRect(color = reminder.priority.color)
            }
            Spacer(modifier = Modifier.width(8.dp))

            Checkbox(
                checked = reminder.isCompleted,
                // --- 核心修正 2：直接将 Checkbox 传出的 Boolean 值传递给回调 ---
                onCheckedChange = onCheckedChange
            )
            Spacer(modifier = Modifier.width(16.dp))

            AnimatedContent(
                targetState = reminder.isCompleted,
                label = "ReminderTextAnimation",
                transitionSpec = {
                    (slideInVertically { height -> height } + fadeIn())
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                }
            ) { isCompleted ->
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
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
    }
}

private fun formatDateTime(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}