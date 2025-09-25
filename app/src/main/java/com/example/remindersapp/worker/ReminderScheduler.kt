package com.example.remindersapp.worker

import android.content.Context
import androidx.work.*
import com.example.remindersapp.data.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// --- 步骤 1：将接口定义合并到此文件顶部 ---
/**
 * 定义了调度后台任务的契约.
 * 这是一个接口，方便在测试中替换为模拟实现.
 */
interface Scheduler {
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: Int)
}

/**
 * Scheduler 接口的生产环境实现，使用 WorkManager.
 */
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : Scheduler {

    override fun schedule(reminder: Reminder) {
        val reminderTime = reminder.dueDate ?: return
        val currentTime = System.currentTimeMillis()
        val delay = reminderTime - currentTime

        if (delay > 0) {
            val data = workDataOf(
                ReminderWorker.KEY_TITLE to reminder.title,
                ReminderWorker.KEY_CONTENT to reminder.notes
            )

            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag(reminder.id.toString())
                .build()

            // 先取消旧任务，再入队新任务，确保任务总是最新的
            WorkManager.getInstance(context).cancelAllWorkByTag(reminder.id.toString())
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override fun cancel(reminderId: Int) {
        WorkManager.getInstance(context).cancelAllWorkByTag(reminderId.toString())
    }
}