package com.example.remindersapp.worker

import android.content.Context
import androidx.work.*
import com.example.remindersapp.data.Reminder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

interface Scheduler {
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: Int)
}

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
                .build()

            // 使用唯一的 ID 来安排任务，方便之后取消或更新
            WorkManager.getInstance(context).enqueueUniqueWork(
                reminder.id.toString(),
                ExistingWorkPolicy.REPLACE, // 如果已存在同名任务，则替换它
                workRequest
            )
        }
    }

    override fun cancel(reminderId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(reminderId.toString())
    }
}