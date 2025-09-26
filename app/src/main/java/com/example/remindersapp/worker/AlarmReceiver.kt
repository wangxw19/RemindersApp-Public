package com.example.remindersapp.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.remindersapp.MainActivity
import com.example.remindersapp.data.AppState
import com.example.remindersapp.data.ReminderRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_REMINDER_ID = "REMINDER_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CONTENT = "EXTRA_CONTENT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            handleBootCompleted(context)
        } else {
            handleAlarmTrigger(context, intent)
        }
    }

    private fun handleBootCompleted(context: Context) {
        val pendingResult = goAsync()
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            RescheduleEntryPoint::class.java
        )
        val repository = hiltEntryPoint.getReminderRepository()
        val scheduler = hiltEntryPoint.getScheduler()
        val scope = hiltEntryPoint.getCoroutineScope()

        scope.launch {
            try {
                val remindersToReschedule = repository.getFutureIncompleteReminders()
                remindersToReschedule.forEach { reminder ->
                    scheduler.schedule(reminder)
                }
                Log.d("AlarmReceiver", "Successfully rescheduled ${remindersToReschedule.size} reminders after boot.")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error rescheduling reminders after boot", e)
            } finally {
                // 任务完成，释放唤醒锁
                pendingResult.finish()
            }
        }
    }

    private fun handleAlarmTrigger(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
        if (reminderId == -1) return

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "提醒"
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""

        // 手动从 ApplicationContext 获取 Hilt 注入的单例
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AlarmTriggerEntryPoint::class.java
        )
        val appState = hiltEntryPoint.getAppState()

        // 使用 runBlocking 是因为 onReceive 生命周期极短，我们需要在这里同步地获取前台状态
        val isAppInForeground = runBlocking { appState.isAppInForeground.first() }

        if (isAppInForeground) {
            // App 在前台：启动 MainActivity 并传递提醒信息
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_SHOW_RINGING_REMINDER
                putExtra(EXTRA_REMINDER_ID, reminderId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(activityIntent)
        } else {
            // App 在后台：启动 RingtoneService
            val serviceIntent = Intent(context, RingtoneService::class.java).apply {
                putExtra(RingtoneService.EXTRA_REMINDER_ID, reminderId) // 传递ID以便后续操作
                putExtra(RingtoneService.EXTRA_TITLE, title)
                putExtra(RingtoneService.EXTRA_CONTENT, content)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}

// 定义 Hilt 入口点以在 Receiver 中安全地获取单例
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface AlarmTriggerEntryPoint {
    fun getAppState(): AppState
}

// --- 新增：为开机重启任务定义专门的 Hilt 入口点 ---
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface RescheduleEntryPoint {
    fun getReminderRepository(): ReminderRepository
    fun getScheduler(): Scheduler
    fun getCoroutineScope(): CoroutineScope
}