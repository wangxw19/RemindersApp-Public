package com.example.remindersapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 代表数据库中的 "reminders" 表.
 * 这是我们应用的核心数据模型.
 *
 * @param id 唯一标识符，自动生成.
 * @param title 提醒事项的标题，不能为空.
 * @param notes 提醒事项的备注，可以为空.
 * @param dueDate 截止日期和时间，以毫秒级时间戳形式存储，可以为空.
 * @param isCompleted 标记此提醒事项是否已完成.
 * @param priority 优先级 (例如 0:无, 1:低, 2:中, 3:高).
 */
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val notes: String?,
    val dueDate: Long?,
    val isCompleted: Boolean = false,
    val priority: Int = 0
)