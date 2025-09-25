package com.example.remindersapp.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.remindersapp.data.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface Scheduler {
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: Int)
}

class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : Scheduler {
    // 获取系统的 AlarmManager 服务
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(reminder: Reminder) {
        val reminderTime = reminder.dueDate ?: return

        // 创建一个 Intent，用于在闹钟触发时发送广播
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(ReminderWorker.KEY_TITLE, reminder.title)
            putExtra(ReminderWorker.KEY_CONTENT, reminder.notes)
            putExtra("REMINDER_ID", reminder.id)
        }

        // 创建一个 PendingIntent。使用 reminder.id 作为 requestCode 确保每个闹钟都是唯一的
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 检查是否有精确闹钟权限
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        } else {
            // 如果没有权限，可以降级为不精确的闹钟，或引导用户开启权限
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        }
    }

    override fun cancel(reminderId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}