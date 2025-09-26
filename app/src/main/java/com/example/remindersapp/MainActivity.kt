package com.example.remindersapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.remindersapp.ui.AppNavHost
import com.example.remindersapp.worker.AlarmReceiver
import com.example.remindersapp.ui.theme.RemindersAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    companion object {
        const val ACTION_SHOW_RINGING_REMINDER = "ACTION_SHOW_RINGING_REMINDER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            RemindersAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(mainViewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
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