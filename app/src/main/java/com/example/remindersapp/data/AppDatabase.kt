package com.example.remindersapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Reminder::class], version = 2, exportSchema = false) // <-- 版本升为 2
@TypeConverters(PriorityConverter::class) // <-- 注册转换器
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}