package com.example.remindersapp.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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

object AppDestinations {
    const val LIST_ROUTE = "list"
    const val COMPLETED_ROUTE = "completed"
    const val DETAILS_ROUTE = "details"
    const val ITEM_ID_ARG = "itemId"
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = hiltViewModel()
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
        }
    }
}