package com.example.remindersapp.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.List
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.remindersapp.MainViewModel
import com.example.remindersapp.data.Reminder
import com.example.remindersapp.data.ThemeSetting
import com.google.gson.Gson
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
                    icon = { Icon(Icons.Default.List, contentDescription = "All Reminders") },
                    label = { Text("All Reminders") },
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
                    icon = { Icon(Icons.Default.Archive, contentDescription = "Completed") },
                    label = { Text("Completed") },
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
                // --- Added: Data management entry ---
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Data Management") },
                    label = { Text("Data Management") },
                    selected = currentRoute == AppDestinations.SETTINGS_ROUTE,
                    onClick = {
                        navController.navigate(AppDestinations.SETTINGS_ROUTE)
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Trash") },
                    selected = currentRoute == AppDestinations.TRASH_ROUTE,
                    onClick = {
                        navController.navigate(AppDestinations.TRASH_ROUTE) {
                            launchSingleTop = true
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                        }
                        scope.launch { drawerState.close() }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text("Theme Settings", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = { viewModel.updateThemeSetting(ThemeSetting.LIGHT) }) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = "Light Mode",
                            tint = if (currentTheme == ThemeSetting.LIGHT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.updateThemeSetting(ThemeSetting.DARK) }) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Dark Mode",
                            tint = if (currentTheme == ThemeSetting.DARK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.updateThemeSetting(ThemeSetting.SYSTEM) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Follow System",
                            tint = if (currentTheme == ThemeSetting.SYSTEM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Text("Data Management", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall)
                NavigationDrawerItem(
                    label = { Text("Export Data") },
                    selected = false,
                    onClick = { 
                        // Simply close the drawer, no operation performed
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Import Data") },
                    selected = false,
                    onClick = { 
                        // Simply close the drawer, no operation performed
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Reminders") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Open Menu")
                        }
                    },
                    actions = {
                        if (ringingReminder != null) {
                            IconButton(onClick = {
                                ringtonePlayer.stop()
                                appState.stopRinging()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.StopCircle,
                                    contentDescription = "Stop Current Ringtone",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.toggleMuteSetting() }) {
                            if (isMuted) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = "Unmute"
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "Mute App"
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
                Text("Stop")
            }
        }
    }
}