package com.example.remindersapp.worker

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.remindersapp.R

class RingtoneService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var countDownTimer: CountDownTimer? = null

    companion object {
        const val ACTION_STOP_SERVICE = "STOP_RINGTONE_SERVICE"
        const val NOTIFICATION_CHANNEL_ID = "RingtoneChannel"
        const val SERVICE_NOTIFICATION_ID = 123
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CONTENT = "EXTRA_CONTENT"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 处理来自通知的停止请求
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopServiceAndCleanup()
            return START_NOT_STICKY
        }

        // 获取提醒的标题和内容
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "提醒"
        val content = intent?.getStringExtra(EXTRA_CONTENT) ?: "您有一个任务需要处理"

        // 启动前台服务，并显示带“停止”按钮的通知
        startForeground(SERVICE_NOTIFICATION_ID, createNotification(title, content))

        // 开始播放铃声
        startRingtone()

        // 启动1分钟倒计时，到时自动停止
        startCountdown()

        return START_STICKY
    }

    private fun startRingtone() {
        // 确保不会重复创建 MediaPlayer
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.reminder_ringtone).apply {
                isLooping = true // 循环播放
                start()
            }
        }
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(60000, 1000) { // 60秒
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                stopServiceAndCleanup()
            }
        }.start()
    }

    // 统一的停止和清理方法
    private fun stopServiceAndCleanup() {
        countDownTimer?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotification(title: String, content: String): Notification {
        createNotificationChannel()

        // 创建点击 "停止" 按钮后要发送的 Intent
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
            .setOngoing(true) // 使通知变为常驻，无法划掉
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
                // 渠道本身设置为无声，因为我们是手动播放声音
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
        super.onDestroy()
        stopServiceAndCleanup()
    }
}