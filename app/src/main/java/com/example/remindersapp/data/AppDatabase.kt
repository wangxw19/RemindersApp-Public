package com.example.remindersapp.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Reminder::class, TrashReminder::class], version = 3, exportSchema = false) // <-- Version upgraded to 3
@TypeConverters(PriorityConverter::class) // <-- Register converter
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun trashReminderDao(): TrashReminderDao // Added: Trash reminder DAO
}