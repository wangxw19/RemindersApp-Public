package com.example.remindersapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.remindersapp.MainViewModel
import com.example.remindersapp.data.Reminder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel(), // 注入 MainViewModel
    mainContent: @Composable (PaddingValues) -> Unit
) {
    val appState = viewModel.appState
    val ringtonePlayer = viewModel.ringtonePlayer
    val ringingReminder by appState.currentRingingReminder.collectAsState()

    // ... (ModalNavigationDrawer 保持不变，请确保您的版本是最新的)

    Scaffold(
        topBar = { ... }, // TopAppBar 不变
        bottomBar = {
            AnimatedVisibility(
                visible = ringingReminder != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                RingingReminderBottomSheet(
                    reminder = ringingReminder,
                    onStopClick = {
                        ringtonePlayer.stop()
                        appState.stopRinging()
                    }
                )
            }
        }
    ) { innerPadding ->
        mainContent(innerPadding)
    }
}

@Composable
fun RingingReminderBottomSheet(
    reminder: Reminder?,
    onStopClick: () -> Unit
) {
    if (reminder == null) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = reminder.title, style = MaterialTheme.typography.titleMedium)
                if (!reminder.notes.isNullOrBlank()) {
                    Text(text = reminder.notes, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = onStopClick) {
                Text("停止")
            }
        }
    }
}