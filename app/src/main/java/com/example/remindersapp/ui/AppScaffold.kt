package com.example.remindersapp.ui

import androidx.compose.foundation.layout.*
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
                        navController.navigate(AppDestinations.LIST_ROUTE)
                        scope.launch { drawerState.close() }
                    }
                )
                NavigationDrawerItem(
                    label = { Text("已完成") },
                    selected = currentRoute == AppDestinations.COMPLETED_ROUTE,
                    onClick = {
                        navController.navigate(AppDestinations.COMPLETED_ROUTE)
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        // 全局 Scaffold，只包含在所有页面都共用的 TopAppBar
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("提醒事项") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                drawerState.apply { if (isClosed) open() else close() }
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "打开菜单")
                        }
                    }
                )
            },
        ) { innerPadding ->
            mainContent(innerPadding)
        }
    }
}