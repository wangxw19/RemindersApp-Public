package com.example.remindersapp.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.remindersapp.data.ReminderRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra(RingtoneService.EXTRA_REMINDER_ID, -1)
        if (reminderId == -1) {
            Log.e("SnoozeReceiver", "Received snooze intent with invalid reminderId.")
            return
        }

        // 停止铃声服务
        val stopServiceIntent = Intent(context, RingtoneService::class.java).apply {
            action = RingtoneService.ACTION_STOP_SERVICE
        }
        context.startService(stopServiceIntent)

        // 使用 goAsync 处理后台任务
        val pendingResult = goAsync()
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            RescheduleEntryPoint::class.java // 复用之前的 EntryPoint
        )
        val repository = hiltEntryPoint.getReminderRepository()
        val scheduler = hiltEntryPoint.getScheduler()
        val scope = hiltEntryPoint.getCoroutineScope()

        scope.launch {
            try {
                val reminder = repository.getReminderStream(reminderId).filterNotNull().first()
                val calendar = Calendar.getInstance()
                // 设置10分钟后提醒
                calendar.add(Calendar.MINUTE, 10)
                val snoozedReminder = reminder.copy(dueDate = calendar.timeInMillis)

                // 更新数据库并重新调度
                repository.updateReminder(snoozedReminder)
                scheduler.schedule(snoozedReminder)

                Log.d("SnoozeReceiver", "Reminder snoozed for 10 minutes: $reminderId")
            } catch (e: Exception) {
                Log.e("SnoozeReceiver", "Error snoozing reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}