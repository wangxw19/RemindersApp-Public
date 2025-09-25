package com.example.remindersapp.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.remindersapp.R
import kotlinx.coroutines.delay

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_TITLE = "key_title"
        const val KEY_CONTENT = "key_content"
        const val CHANNEL_ID = "reminder_alarm_channel"
        const val FOREGROUND_NOTIFICATION_ID = 111 // 前台服务的固定通知ID
    }

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val content = inputData.getString(KEY_CONTENT) ?: ""

        // --- 核心改动：将 Worker 提升为前台服务以确保执行 ---
        val foregroundNotification = createNotification(title, "正在处理提醒...", true)
        setForeground(ForegroundInfo(FOREGROUND_NOTIFICATION_ID, foregroundNotification))

        // --- 核心改动：直接播放闹钟铃声 ---
        playAlarmSound()

        // 播放一小段时间后停止，或者等待用户交互（更复杂的逻辑）
        // 这里我们简单地延迟几秒，模拟闹钟响铃
        delay(5000) // 响铃5秒

        // --- 发送最终的用户可见通知 ---
        // 注意：这个通知是无声的，因为声音我们已经手动播放了
        val userVisibleNotification = createNotification(title, content, false)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), userVisibleNotification)

        return Result.success()
    }

    private fun playAlarmSound() {
        try {
            val alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alarmSoundUri)
            ringtone.play()
            // 注意：默认的铃声可能会一直循环，我们需要在合适的时候 stop()
            // 但在这个 Worker 的生命周期内，它会在 doWork() 结束后自动停止。
            // 为了确保它能响一段时间，我们在 doWork() 中加了 delay。
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(title: String, content: String, isForegroundServiceNotification: Boolean): android.app.Notification {
        createNotificationChannel() // 确保渠道已创建

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(isForegroundServiceNotification) // 前台服务通知只响一次（实际上我们让它无声）

        if (!isForegroundServiceNotification) {
            // 用户可见的通知应该可以自动取消，并且有震动
            builder.setAutoCancel(true)
                .setVibrate(longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400))
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "闹钟提醒"
            val descriptionText = "重要的提醒事项通知"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // 让通知渠道本身无声，因为我们手动播放声音
                setSound(null, null)
                enableVibration(true) // 震动依然由渠道控制
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}