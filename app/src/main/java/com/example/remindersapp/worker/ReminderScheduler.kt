package com.example.remindersapp.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.remindersapp.data.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 定义了调度后台任务的契约.
 * 这是一个接口，方便在测试中替换为模拟实现.
 */
interface Scheduler {
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: Int)
}

/**
 * Scheduler 接口的生产环境实现，使用 AlarmManager 实现精确的定时调度.
 */
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : Scheduler {

    // 从系统服务中获取 AlarmManager 实例
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(reminder: Reminder) {
        // 确保任务有截止日期
        val reminderTime = reminder.dueDate ?: return

        // 创建一个指向 AlarmReceiver 的 Intent，用于在闹钟触发时发送广播
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            // 将标题和内容放入 Intent extras，供后续的 Service 使用
            // 使用 RingtoneService 中定义的常量，确保 Key 的一致性
            putExtra(RingtoneService.EXTRA_TITLE, reminder.title)
            putExtra(RingtoneService.EXTRA_CONTENT, reminder.notes)
        }

        // 创建一个 PendingIntent。
        // 使用 reminder.id 作为 requestCode 确保每个闹钟都是唯一的，这样我们才能精确地更新或取消它。
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            // FLAG_UPDATE_CURRENT: 如果已存在相同 requestCode 的 PendingIntent，则用新的 Intent 更新它的 extra 数据。
            // FLAG_IMMUTABLE: 出于安全考虑，声明这个 PendingIntent 的内容是不可变的。
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 在设置闹钟前，检查应用是否拥有精确闹钟权限 (适用于 Android 12+)
        // canScheduleExactAlarms() 是 Android 12 (API 31) 引入的
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                // 拥有权限，设置一个高精度的、能在设备空闲时唤醒的闹钟
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                // 没有权限，降级为不精确的闹钟。
                // 此时，应用应该在详情页引导用户去开启权限。
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            }
        } else {
            // 对于 Android 12 以下的旧版本，直接设置精确闹钟
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    override fun cancel(reminderId: Int) {
        // 创建一个与 schedule 方法中完全一致的 Intent 和 PendingIntent
        // 这样系统才能通过 .equals() 方法找到并取消它
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // 取消与此 PendingIntent 关联的任何闹钟
        alarmManager.cancel(pendingIntent)
    }
}