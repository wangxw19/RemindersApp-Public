package com.example.remindersapp.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 从 AlarmManager 的 Intent 中提取数据
        val title = intent.getStringExtra(RingtoneService.EXTRA_TITLE) ?: "提醒"
        val content = intent.getStringExtra(RingtoneService.EXTRA_CONTENT) ?: ""

        // 创建启动 RingtoneService 的 Intent
        val serviceIntent = Intent(context, RingtoneService::class.java).apply {
            putExtra(RingtoneService.EXTRA_TITLE, title)
            putExtra(RingtoneService.EXTRA_CONTENT, content)
        }

        // 使用 ContextCompat.startForegroundService 来安全地启动服务
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}