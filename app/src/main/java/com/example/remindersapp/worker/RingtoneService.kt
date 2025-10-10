package com.example.remindersapp.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.remindersapp.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 优化要点：
 * 1. 添加完整的生命周期管理
 * 2. 错误处理和日志
 * 3. 使用协程而非阻塞调用
 * 4. 防止内存泄漏
 */
@AndroidEntryPoint
class RingtoneService : Service() {

    @Inject
    lateinit var ringtonePlayer: RingtonePlayer

    private var currentReminderId: Int = -1
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_STOP_SERVICE = "STOP_RINGTONE_SERVICE"
        const val NOTIFICATION_CHANNEL_ID = "RingtoneChannel"
        const val SERVICE_NOTIFICATION_ID = 123
        const val EXTRA_REMINDER_ID = "EXTRA_REMINDER_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CONTENT = "EXTRA_CONTENT"
        private const val TAG = "RingtoneService"
        private const val DEBUG = false
    }

    override fun onCreate() {
        super.onCreate()
        if (DEBUG) Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (DEBUG) Log.d(TAG, "onStartCommand: action=${intent?.action}")

        if (intent?.action == ACTION_STOP_SERVICE) {
            stopServiceAndCleanup()
            return START_NOT_STICKY
        }

        try {
            val title = intent?.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() } ?: "提醒"
            val content = intent?.getStringExtra(EXTRA_CONTENT)?.takeIf { it.isNotBlank() } ?: ""
            currentReminderId = intent?.getIntExtra(EXTRA_REMINDER_ID, -1) ?: -1

            if (currentReminderId == -1) {
                Log.w(TAG, "Started with invalid reminder ID, stopping")
                stopServiceAndCleanup()
                return START_NOT_STICKY
            }

            // 创建前台通知（必须在 startForeground 之前）
            val notification = createNotification(title, content)
            startForeground(SERVICE_NOTIFICATION_ID, notification)

            // 异步启动铃声播放
            serviceScope.launch {
                ringtonePlayer.startIfNotMuted { success ->
                    if (DEBUG) Log.d(TAG, "Ringtone start result: $success")
                }
            }

            if (DEBUG) Log.d(TAG, "Service started for reminder $currentReminderId")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
            stopServiceAndCleanup()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    /**
     * 停止服务并清理资源
     */
    private fun stopServiceAndCleanup() {
        if (DEBUG) Log.d(TAG, "Stopping service and cleaning up")

        try {
            ringtonePlayer.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ringtone player", e)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping foreground", e)
        }

        stopSelf()
    }

    /**
     * 创建前台通知
     */
    private fun createNotification(title: String, content: String): Notification {
        createNotificationChannel()

        // 停止服务的 Intent
        val stopSelf = Intent(this, RingtoneService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pStopSelf = PendingIntent.getService(
            this, 0, stopSelf,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 打盹的 Intent（仅在有效 ID 时添加）
        var pSnooze: PendingIntent? = null
        if (currentReminderId != -1) {
            val snoozeIntent = Intent(this, SnoozeReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, currentReminderId)
            }
            pSnooze = PendingIntent.getBroadcast(
                this, currentReminderId, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_notification, "停止响铃", pStopSelf)

        // 只在有效 ID 时添加打盹按钮
        if (pSnooze != null) {
            builder.addAction(R.drawable.ic_snooze, "打盹10分钟", pSnooze)
        }

        return builder.build()
    }

    /**
     * 创建通知渠道（Android 8+ 需要）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val serviceChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "提醒铃声服务",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                    description = "前台提醒服务"
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(serviceChannel)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (DEBUG) Log.d(TAG, "Service destroyed")
        try {
            ringtonePlayer.onDestroy()
            serviceScope.launch {
                // 等待回调完成后清理
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
        super.onDestroy()
    }
}