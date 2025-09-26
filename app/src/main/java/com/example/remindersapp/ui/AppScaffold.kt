package com.example.remindersapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.Notifications
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
import com.example.remindersapp.data.ThemeSetting
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavController,
    viewModel: MainViewModel,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val appState = viewModel.appState
    val ringtonePlayer = viewModel.ringtonePlayer
    val ringingReminder by appState.currentRingingReminder.collectAsState()
    val currentTheme by viewModel.themeSetting.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()

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
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text("主题设置", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = { viewModel.updateThemeSetting(ThemeSetting.LIGHT) }) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = "浅色模式",
                            tint = if (currentTheme == ThemeSetting.LIGHT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.updateThemeSetting(ThemeSetting.DARK) }) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "深色模式",
                            tint = if (currentTheme == ThemeSetting.DARK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.updateThemeSetting(ThemeSetting.SYSTEM) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "跟随系统",
                            tint = if (currentTheme == ThemeSetting.SYSTEM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                    },
                    // --- 新增：顶部操作按钮 ---
                    actions = {
                        // 1. 停止当前铃声按钮 (仅在响铃时显示)
                        if (ringingReminder != null) {
                            IconButton(onClick = {
                                ringtonePlayer.stop()
                                appState.stopRinging()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.StopCircle,
                                    contentDescription = "停止当前铃声",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // 2. 应用级静音切换按钮
                        IconButton(onClick = { viewModel.toggleMuteSetting() }) {
                            if (isMuted) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = "取消静音"
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "静音应用"
                                )
                            }
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
            },
            content = content
        )
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