package com.example.remindersapp.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.remindersapp.R

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_TITLE = "key_title"
        const val KEY_CONTENT = "key_content"
        const val CHANNEL_ID = "reminder_notification_channel" // 使用新的、更合适的渠道ID
    }

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val content = inputData.getString(KEY_CONTENT) ?: ""

        showNotification(title, content)

        return Result.success()
    }

    private fun showNotification(title: String, content: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // --- 核心改动：使用系统默认的【通知】铃声 ---
        val notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "任务提醒"
            val descriptionText = "提醒事项通知"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                // 在渠道中明确设置声音和震动
                setSound(notificationSoundUri, null) // AudioAttributes 设为 null，使用默认通知属性
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250) // 一个简单有效的震动模式
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // 为旧版本安卓设置默认声音和震动（会自动选择通知音和默认震动模式）
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}