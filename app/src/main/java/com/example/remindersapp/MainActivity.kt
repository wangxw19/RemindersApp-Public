package com.example.remindersapp

import android.content.Intent
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
import com.example.remindersapp.worker.AlarmReceiver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    companion object {
        const val ACTION_SHOW_RINGING_REMINDER = "ACTION_SHOW_RINGING_REMINDER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 处理首次启动时的 Intent
        handleIntent(intent)

        setContent {
            // ... (setContent 内容保持不变)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 当 Activity 已在运行时，处理新的 Intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_RINGING_REMINDER) {
            val reminderId = intent.getIntExtra(AlarmReceiver.EXTRA_REMINDER_ID, -1)
            if (reminderId != -1) {
                viewModel.triggerInAppRinging(reminderId)
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