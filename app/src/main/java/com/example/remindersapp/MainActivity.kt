package com.example.remindersapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.remindersapp.ui.AppNavHost
import com.example.remindersapp.ui.AppScaffold
import com.example.remindersapp.ui.theme.RemindersAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // --- 核心改动 1：使用 activity-ktx 的 viewModels() 委托来获取 ViewModel ---
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemindersAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // --- 核心改动 2：将 Activity 持有的 ViewModel 实例作为参数传递下去 ---
                    AppScaffold(
                        navController = navController,
                        viewModel = viewModel
                    ) { innerPadding ->
                        AppNavHost(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.appState.setAppInForeground(true)
    }

    override fun onStop() {
        super.onStop()
        viewModel.appState.setAppInForeground(false)
    }
}