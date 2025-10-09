package com.example.remindersapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    // 注意：我们不使用@Delete，而是设置isDeleted标志
    @Query("UPDATE reminders SET isDeleted = 1 WHERE id = :id")
    suspend fun markAsDeleted(id: Int)

    @Query("UPDATE reminders SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: Int)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deletePermanently(id: Int)

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getReminderById(id: Int): Flow<Reminder?>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isDeleted = 0 ORDER BY dueDate ASC, priority DESC")
    fun getIncompleteReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND isDeleted = 0 ORDER BY dueDate DESC")
    fun getCompletedReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isDeleted = 1 ORDER BY dueDate DESC")
    fun getDeletedReminders(): Flow<List<Reminder>>

    // --- 新增：用于开机重启时，获取所有未完成且时间在未来的提醒 ---
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND dueDate > :currentTimeMillis AND isDeleted = 0")
    suspend fun getFutureIncompleteReminders(currentTimeMillis: Long): List<Reminder>

    // 获取所有提醒（包括已删除的）
    @Query("SELECT * FROM reminders")
    suspend fun getAllReminders(): List<Reminder>

    // 批量删除提醒
    @Query("DELETE FROM reminders WHERE isDeleted = 1")
    suspend fun deleteAllPermanently()
}