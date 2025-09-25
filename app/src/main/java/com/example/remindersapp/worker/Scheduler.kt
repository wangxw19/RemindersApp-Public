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
                .addTag(reminder.id.toString()) // 使用 Tag 来标识任务
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override fun cancel(reminderId: Int) {
        // 通过 Tag 来取消任务
        WorkManager.getInstance(context).cancelAllWorkByTag(reminderId.toString())
    }
}
