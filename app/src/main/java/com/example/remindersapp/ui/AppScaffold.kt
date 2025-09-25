package com.example.remindersapp.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavController,
    mainContent: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                // 可以在这里添加更多菜单项，比如“设置”
            }
        }
    ) {
        // Scaffold 已被我们废弃，因为我们将在每个屏幕内部根据需要提供它
        // 这里直接调用 mainContent
        mainContent(PaddingValues(0.dp)) // 传递一个空的 PaddingValues
    }
}