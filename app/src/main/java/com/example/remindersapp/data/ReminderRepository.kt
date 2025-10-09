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
    suspend fun deleteReminder(reminder: Reminder) // 软删除
    suspend fun deleteReminderPermanently(id: Int) // 永久删除
    suspend fun restoreReminder(id: Int) // 恢复提醒
    fun getCompletedRemindersStream(): Flow<List<Reminder>>
    fun getDeletedRemindersStream(): Flow<List<Reminder>> // 获取已删除的提醒
    // --- 新增接口方法 ---
    suspend fun getFutureIncompleteReminders(): List<Reminder>
    suspend fun getAllReminders(): List<Reminder>
    suspend fun saveAllReminders(reminders: List<Reminder>)
}

class OfflineReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val trashReminderDao: TrashReminderDao // 新增：注入回收站DAO
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

    // 软删除：将提醒移动到回收站并标记为已删除
    override suspend fun deleteReminder(reminder: Reminder) {
        // 首先保存到回收站
        val trashReminder = TrashReminder(
            originalId = reminder.id,
            title = reminder.title,
            notes = reminder.notes,
            dueDate = reminder.dueDate,
            isCompleted = reminder.isCompleted,
            priority = reminder.priority
        )
        trashReminderDao.insert(trashReminder)
        
        // 然后在主表中标记为已删除
        reminderDao.markAsDeleted(reminder.id)
    }

    override suspend fun deleteReminderPermanently(id: Int) {
        reminderDao.deletePermanently(id)
    }

    override suspend fun restoreReminder(id: Int) {
        reminderDao.restoreFromTrash(id)
        // 从回收站删除对应的记录（如果存在）
        trashReminderDao.deleteById(id)
    }

    override fun getCompletedRemindersStream(): Flow<List<Reminder>> =
        reminderDao.getCompletedReminders()

    override fun getDeletedRemindersStream(): Flow<List<Reminder>> =
        reminderDao.getDeletedReminders()

    // --- 新增实现 ---
    override suspend fun getFutureIncompleteReminders(): List<Reminder> {
        return reminderDao.getFutureIncompleteReminders(System.currentTimeMillis())
    }

    override suspend fun getAllReminders(): List<Reminder> {
        return reminderDao.getAllReminders()
    }

    override suspend fun saveAllReminders(reminders: List<Reminder>) {
        // 清除现有数据并插入新数据
        reminderDao.deleteAllPermanently()
        for (reminder in reminders) {
            reminderDao.insert(reminder)
        }
    }
}