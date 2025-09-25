package com.example.remindersapp.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.remindersapp.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RingtoneService : Service() {

    @Inject
    lateinit var ringtonePlayer: RingtonePlayer

    // 移除倒计时器，因为通知将一直存在直到用户交互
    // private var countDownTimer: CountDownTimer? = null

    companion object {
        const val ACTION_STOP_SERVICE = "STOP_RINGTONE_SERVICE"
        const val NOTIFICATION_CHANNEL_ID = "RingtoneChannel"
        const val SERVICE_NOTIFICATION_ID = 123
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CONTENT = "EXTRA_CONTENT"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopServiceAndCleanup()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "提醒"
        val content = intent?.getStringExtra(EXTRA_CONTENT) ?: "您有一个任务需要处理"

        startForeground(SERVICE_NOTIFICATION_ID, createNotification(title, content))
        ringtonePlayer.start()

        return START_STICKY
    }

    private fun stopServiceAndCleanup() {
        ringtonePlayer.stop()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        ringtonePlayer.stop()
        super.onDestroy()
    }
}