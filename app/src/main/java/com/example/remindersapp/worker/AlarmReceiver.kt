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

    // 使用 IO 调度器进行数据库查询
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        // --- 核心改动 1：获取 PendingResult，阻止 Receiver 被立即杀死 ---
        val pendingResult = goAsync()

        val reminderId = intent.getIntExtra("REMINDER_ID", -1)
        if (reminderId == -1) {
            pendingResult.finish() // 如果 ID 无效，也要结束
            return
        }

        scope.launch {
            try {
                // 异步从数据库获取提醒详情
                val reminder = repository.getReminderStream(reminderId).first()

                if (reminder != null) {
                    // 判断 App 是否在前台
                    if (appState.isAppInForeground.value) {
                        // 在前台：直接播放铃声，并更新全局 UI 状态
                        ringtonePlayer.start()
                        appState.startRinging(reminder)
                    } else {
                        // 在后台：启动 RingtoneService，通过通知提醒
                        val serviceIntent = Intent(context, RingtoneService::class.java).apply {
                            putExtra(RingtoneService.EXTRA_TITLE, reminder.title)
                            putExtra(RingtoneService.EXTRA_CONTENT, reminder.notes)
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                }
            } finally {
                // --- 核心改动 2：无论成功与否，都要调用 finish()，通知系统异步工作已完成 ---
                pendingResult.finish()
            }
        }
    }
}