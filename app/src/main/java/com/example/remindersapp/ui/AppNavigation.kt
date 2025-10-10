package com.example.remindersapp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.remindersapp.MainViewModel
import com.example.remindersapp.ui.completed.CompletedListScreen
import com.example.remindersapp.ui.details.ReminderDetailsScreen
import com.example.remindersapp.ui.list.ReminderListScreen
import com.example.remindersapp.ui.search.SearchScreen
import com.example.remindersapp.ui.settings.SettingsScreen
import com.example.remindersapp.ui.trash.TrashListScreen

object AppDestinations {
    const val LIST_ROUTE = "list"
    const val COMPLETED_ROUTE = "completed"
    const val DETAILS_ROUTE = "details"
    const val TRASH_ROUTE = "trash"
    const val SETTINGS_ROUTE = "settings"
    const val SEARCH_ROUTE = "search"
    const val ITEM_ID_ARG = "itemId"
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel
) {
    AppScaffold(navController = navController, viewModel = mainViewModel) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.LIST_ROUTE,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(route = AppDestinations.LIST_ROUTE) {
                ReminderListScreen(
                    onItemClick = { reminderId ->
                        navController.navigate("${AppDestinations.DETAILS_ROUTE}/$reminderId")
                    },
                    onFabClick = {
                        navController.navigate("${AppDestinations.DETAILS_ROUTE}/-1")
                    }
                )
            }

            composable(
                route = "${AppDestinations.DETAILS_ROUTE}/{${AppDestinations.ITEM_ID_ARG}}",
                arguments = listOf(navArgument(AppDestinations.ITEM_ID_ARG) { type = NavType.IntType })
            ) {
                ReminderDetailsScreen(
                    onNavigateUp = { navController.navigateUp() }
                )
            }

            composable(route = AppDestinations.COMPLETED_ROUTE) {
                CompletedListScreen(
                    onItemClick = { reminderId ->
                        navController.navigate("${AppDestinations.DETAILS_ROUTE}/$reminderId")
                    }
                )
            }
            
            composable(route = AppDestinations.TRASH_ROUTE) {
                TrashListScreen(
                    viewModel = mainViewModel
                )
            }
            
            composable(route = AppDestinations.SETTINGS_ROUTE) {
                SettingsScreen(
                    onNavigateUp = { navController.navigateUp() }
                )
            }
            
            composable(route = AppDestinations.SEARCH_ROUTE) {
                SearchScreen(
                    onNavigateUp = { navController.navigateUp() },
                    onItemClick = { reminderId ->
                        navController.navigate("${AppDestinations.DETAILS_ROUTE}/$reminderId")
                    }
                )
            }
        }
    }
}