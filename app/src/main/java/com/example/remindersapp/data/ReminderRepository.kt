package com.example.remindersapp.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ReminderRepository {
    fun getIncompleteRemindersStream(): Flow<List<Reminder>>
    fun getReminderStream(id: Int): Flow<Reminder?>
    suspend fun insertReminder(reminder: Reminder): Long
    suspend fun updateReminder(reminder: Reminder)
    suspend fun deleteReminder(reminder: Reminder)
    fun getCompletedRemindersStream(): Flow<List<Reminder>>
}

class OfflineReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao
) : ReminderRepository {
    override fun getIncompleteRemindersStream(): Flow<List<Reminder>> =
        reminderDao.getIncompleteReminders()

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
}