package com.example.remindersapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Reminder::class, TrashReminder::class], version = 3, exportSchema = false) // <-- 版本升为 3
@TypeConverters(PriorityConverter::class) // <-- 注册转换器
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun trashReminderDao(): TrashReminderDao // 新增：回收站提醒事项DAO
}