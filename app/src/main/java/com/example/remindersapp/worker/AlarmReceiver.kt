package com.example.remindersapp.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 从 Intent 中获取传递过来的数据
        val title = intent.getStringExtra(ReminderWorker.KEY_TITLE) ?: "提醒"
        val content = intent.getStringExtra(ReminderWorker.KEY_CONTENT) ?: ""
        val reminderId = intent.getIntExtra("REMINDER_ID", 0)

        // 创建一个零延迟的 WorkManager 任务
        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf(
                ReminderWorker.KEY_TITLE to title,
                ReminderWorker.KEY_CONTENT to content
            ))
            .addTag(reminderId.toString())
            .build()

        // 立即执行任务
        WorkManager.getInstance(context).enqueue(workRequest)

        // 如果是重启广播，需要重新调度所有闹钟 (此部分逻辑较复杂，可作为后续优化)
    }
}