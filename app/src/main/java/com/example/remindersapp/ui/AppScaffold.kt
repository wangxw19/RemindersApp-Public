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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.remindersapp.MainViewModel
import com.example.remindersapp.data.Reminder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavController,
    viewModel: MainViewModel, // --- 核心改动 1：通过参数接收 ViewModel ---
    mainContent: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // --- 核心改动 2：直接从传入的 viewModel 中获取状态和播放器 ---
    val appState = viewModel.appState
    val ringtonePlayer = viewModel.ringtonePlayer
    val ringingReminder by appState.currentRingingReminder.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("所有提醒") },
                    selected = currentRoute == AppDestinations.LIST_ROUTE,
                    onClick = {
                        navController.navigate(AppDestinations.LIST_ROUTE) {
                            launchSingleTop = true
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                        }
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("已完成") },
                    selected = currentRoute == AppDestinations.COMPLETED_ROUTE,
                    onClick = {
                        navController.navigate(AppDestinations.COMPLETED_ROUTE) {
                            launchSingleTop = true
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                        }
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("提醒事项") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "打开菜单")
                        }
                    }
                )
            },
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