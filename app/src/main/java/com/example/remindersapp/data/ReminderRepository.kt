package com.example.remindersapp.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

interface ReminderRepository {
    fun getIncompleteRemindersStream(): Flow<List<Reminder>>
    fun getReminderStream(id: Int): Flow<Reminder?>
    suspend fun insertReminder(reminder: Reminder): Long
    suspend fun updateReminder(reminder: Reminder)
    suspend fun deleteReminder(reminder: Reminder)
    fun getCompletedRemindersStream(): Flow<List<Reminder>>
    // --- 新增接口方法 ---
    suspend fun getFutureIncompleteReminders(): List<Reminder>
}

class OfflineReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao
) : ReminderRepository {
    override fun getIncompleteRemindersStream(): Flow<List<Reminder>> {
        Log.d("AppDebug", "Repository: getIncompleteRemindersStream CALLED")
        return reminderDao.getIncompleteReminders().onEach { reminders ->
            Log.d("AppDebug", "Repository: Flow EMITTED ${reminders.size} items from DB")
        }
    }

    override fun getReminderStream(id: Int): Flow<Reminder?> =
        reminderDao.getReminderById(id)

    override suspend fun insertReminder(reminder: Reminder): Long =
        reminderDao.insert(reminder)

    override suspend fun updateReminder(reminder: Reminder) =
        reminderDao.update(reminder)

    override suspend fun deleteReminder(reminder: Reminder) =
        reminderDao.delete(reminder)

    override fun getCompletedRemindersStream(): Flow<List<Reminder>> =
        reminderDao.getCompletedReminders()

    // --- 新增实现 ---
    override suspend fun getFutureIncompleteReminders(): List<Reminder> {
        return reminderDao.getFutureIncompleteReminders(System.currentTimeMillis())
    }
}