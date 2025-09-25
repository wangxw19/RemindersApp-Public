package com.example.remindersapp.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.remindersapp.data.AppState
import com.example.remindersapp.data.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var ringtonePlayer: RingtonePlayer

    @Inject
    lateinit var repository: ReminderRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("REMINDER_ID", -1)
        if (reminderId == -1) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val reminder = repository.getReminderStream(reminderId).first() ?: return@launch

                if (appState.isAppInForeground.value) {
                    ringtonePlayer.start()
                    appState.startRinging(reminder)
                } else {
                    val serviceIntent = Intent(context, RingtoneService::class.java).apply {
                        putExtra(RingtoneService.EXTRA_TITLE, reminder.title)
                        putExtra(RingtoneService.EXTRA_CONTENT, reminder.notes)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}