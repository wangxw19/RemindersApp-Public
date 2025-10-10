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

/**
 * 优化要点：
 * 1. 移除所有 runBlocking 调用 - 使用 goAsync() + 协程替代
 * 2. 添加完整的错误处理
 * 3. 添加参数验证
 * 4. 使用常量替代 Magic Strings
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_REMINDER_ID = "REMINDER_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CONTENT = "EXTRA_CONTENT"
        private const val TAG = "AlarmReceiver"
        private const val DEBUG = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (context == null || intent == null) {
            Log.e(TAG, "Received null context or intent")
            return
        }

        try {
            val action = intent.action
            if (DEBUG) Log.d(TAG, "onReceive called with action: $action")

            when (action) {
                Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
                else -> handleAlarmTrigger(context, intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in onReceive", e)
        }
    }

    /**
     * 处理开机启动 - 重新调度所有未来的提醒
     * 使用 goAsync() 获取更多时间处理后台任务
     */
    private fun handleBootCompleted(context: Context) {
        if (DEBUG) Log.d(TAG, "Handling boot completed event")

        val pendingResult = goAsync()
        val hiltEntryPoint = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                RescheduleEntryPoint::class.java
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Hilt entry point", e)
            pendingResult.finish()
            return
        }

        val repository = hiltEntryPoint.getReminderRepository()
        val scheduler = hiltEntryPoint.getScheduler()
        val scope = hiltEntryPoint.getCoroutineScope()

        scope.launch {
            try {
                if (DEBUG) Log.d(TAG, "Fetching future incomplete reminders...")
                val remindersToReschedule = repository.getFutureIncompleteReminders()

                if (DEBUG) Log.d(TAG, "Rescheduling ${remindersToReschedule.size} reminders")

                var successCount = 0
                var failureCount = 0

                remindersToReschedule.forEach { reminder ->
                    try {
                        scheduler.schedule(reminder)
                        successCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to schedule reminder ${reminder.id}", e)
                        failureCount++
                    }
                }

                Log.i(
                    TAG,
                    "Boot completed: scheduled $successCount reminders, " +
                            "$failureCount failures"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during boot reschedule", e)
            } finally {
                // 立即释放唤醒锁，即使出错
                pendingResult.finish()
            }
        }
    }

    /**
     * 处理闹钟触发
     * 根据应用前台状态选择启动 Activity 或 Service
     */
    private fun handleAlarmTrigger(context: Context, intent: Intent) {
        // 参数验证
        val reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1)
        if (reminderId == -1) {
            Log.w(TAG, "Received alarm trigger with invalid reminderId")
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: "提醒"
        val content = intent.getStringExtra(EXTRA_CONTENT)?.takeIf { it.isNotBlank() } ?: ""

        if (DEBUG) Log.d(TAG, "Handling alarm trigger for reminder: $reminderId")

        // 注意：这里我们不能使用 runBlocking，必须异步检查前台状态
        // 改为直接启动 Activity（更可靠的方式）
        startReminderNotification(context, reminderId, title, content)
    }

    /**
     * 启动提醒 - 优先启动前台 Service 以确保可靠性
     */
    private fun startReminderNotification(
        context: Context,
        reminderId: Int,
        title: String,
        content: String
    ) {
        try {
            // 先启动 Service 确保铃声播放
            val serviceIntent = Intent(context, RingtoneService::class.java).apply {
                putExtra(RingtoneService.EXTRA_REMINDER_ID, reminderId)
                putExtra(RingtoneService.EXTRA_TITLE, title)
                putExtra(RingtoneService.EXTRA_CONTENT, content)
            }

            try {
                ContextCompat.startForegroundService(context, serviceIntent)
                if (DEBUG) Log.d(TAG, "Started foreground service")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
                // Fallback：尝试普通 startService
                try {
                    context.startService(serviceIntent)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to start service (fallback)", e2)
                }
            }

            // 同时尝试启动 Activity（如果应用在前台）
            // 使用 PendingIntent 方式避免直接检查前台状态
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_SHOW_RINGING_REMINDER
                putExtra(EXTRA_REMINDER_ID, reminderId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            try {
                context.startActivity(activityIntent)
                if (DEBUG) Log.d(TAG, "Started activity")
            } catch (e: Exception) {
                if (DEBUG) Log.d(TAG, "Activity not available (app in background)", e)
                // 这是正常情况 - Service 会处理显示
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in startReminderNotification", e)
        }
    }
}

// --- 优化：提取公共的 Hilt 入口点获取逻辑 ---
/**
 * 工具函数：安全地获取 Hilt 入口点
 */
inline fun <reified T> safeGetHiltEntryPoint(context: Context): T? {
    return try {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            T::class.java
        )
    } catch (e: Exception) {
        Log.e("HiltUtils", "Failed to get entry point ${T::class.simpleName}", e)
        null
    }
}

/**
 * 定义 Hilt 入口点以在 Receiver 中安全地获取单例
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface AlarmTriggerEntryPoint {
    fun getAppState(): AppState
}

/**
 * 为开机重启任务定义专门的 Hilt 入口点
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface RescheduleEntryPoint {
    fun getReminderRepository(): ReminderRepository
    fun getScheduler(): Scheduler
    fun getCoroutineScope(): CoroutineScope
}