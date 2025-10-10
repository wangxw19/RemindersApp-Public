package com.example.remindersapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable

// --- MODIFIED: Add @Serializable annotation ---
@Serializable
@Entity(tableName = "reminders")
@TypeConverters(PriorityConverter::class) // <-- 应用转换器
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val notes: String?,
    val dueDate: Long?,
    val isCompleted: Boolean = false,
    val isDeleted: Boolean = false, // 新增：删除状态
    val priority: Priority // <-- 修改类型
)