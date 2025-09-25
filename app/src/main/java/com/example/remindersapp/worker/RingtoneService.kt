package com.example.remindersapp.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.remindersapp.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// --- 新增：使用 Hilt 注入 Service ---
@AndroidEntryPoint
class RingtoneService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    // 不再需要倒计时器
    // private var countDownTimer: CountDownTimer? = null

    companion object {
        const val ACTION_STOP_SERVICE = "STOP_RINGTONE_SERVICE"
        const val NOTIFICATION_CHANNEL_ID = "RingtoneChannel"
        const val SERVICE_NOTIFICATION_ID = 123
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CONTENT = "EXTRA_CONTENT"
    }

    override fun onCreate() {
        super.onCreate()
        // --- 核心改动：在 onCreate 中准备 MediaPlayer ---
        mediaPlayer = MediaPlayer.create(this, R.raw.reminder_ringtone).apply {
            isLooping = true // 循环播放
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 处理来自通知的停止请求
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopServiceAndCleanup()
            return START_NOT_STICKY // 服务不再需要，不自动重启
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "提醒"
        val content = intent?.getStringExtra(EXTRA_CONTENT) ?: "您有一个任务需要处理"

        startForeground(SERVICE_NOTIFICATION_ID, createNotification(title, content))

        // 开始播放铃声
        mediaPlayer?.start()

        // --- 核心改动：移除倒计时器 ---
        // startCountdown() // 不再需要自动停止

        // START_STICKY 确保如果服务被系统杀死，系统会尝试重新创建它
        return START_STICKY
    }

    // 统一的停止和清理方法
    private fun stopServiceAndCleanup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            // 对于旧版本，true 会移除通知
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotification(title: String, content: String): Notification {
        createNotificationChannel()

        val stopSelf = Intent(this, RingtoneService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pStopSelf = PendingIntent.getService(
            this, 0, stopSelf,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            // --- 关键：确保通知是常驻的 ---
            .setOngoing(true)
            .addAction(R.drawable.ic_notification, "停止响铃", pStopSelf)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "提醒铃声服务",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        // --- 核心改动：确保在服务销毁时彻底释放 MediaPlayer ---
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}