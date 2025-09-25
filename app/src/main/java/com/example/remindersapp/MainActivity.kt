package com.example.remindersapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RemindersAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    // 注意：现在 AppScaffold 应该能被正确识别了
                    AppScaffold(navController = navController) {
                        // 我们将 AppNavHost 放在这里，不再传递 PaddingValues
                        AppNavHost(navController = navController)
                    }
                }
            }
        }
    }
}