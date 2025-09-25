package com.example.remindersapp.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
    }

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val content = inputData.getString(KEY_CONTENT) ?: ""

        showNotification(title, content)

        return Result.success()
    }

    private fun showNotification(title: String, content: String) {
        val channelId = "reminder_channel"
        val notificationId = System.currentTimeMillis().toInt()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "提醒事项"
            val descriptionText = "提醒事项通知"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}