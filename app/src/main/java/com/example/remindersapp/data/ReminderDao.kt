package com.example.remindersapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminderById(id: Int): Flow<Reminder?>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY dueDate ASC, priority DESC")
    fun getIncompleteReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY dueDate DESC")
    fun getCompletedReminders(): Flow<List<Reminder>>

    // --- 新增：用于开机重启时，获取所有未完成且时间在未来的提醒 ---
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND dueDate > :currentTimeMillis")
    suspend fun getFutureIncompleteReminders(currentTimeMillis: Long): List<Reminder>
}