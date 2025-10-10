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
    // --- Added interface methods ---
    suspend fun getFutureIncompleteReminders(): List<Reminder>
    suspend fun getAllReminders(): List<Reminder>  // Added for backup
    suspend fun replaceAllReminders(reminders: List<Reminder>)  // Added for backup
    suspend fun appendReminders(reminders: List<Reminder>)  // Added for appending import data
    fun searchActiveReminders(query: String): Flow<List<Reminder>>  // Added for search functionality
    fun searchCompletedReminders(query: String): Flow<List<Reminder>>  // Added for search functionality
    fun searchTrashReminders(query: String): Flow<List<TrashReminder>>  // Added for search functionality
    
    // --- Trash-related methods ---
    fun getAllTrashRemindersStream(): Flow<List<TrashReminder>>
    suspend fun insertTrashReminder(trashReminder: TrashReminder)
    suspend fun updateTrashReminder(trashReminder: TrashReminder)
    suspend fun deleteTrashReminderById(id: Int)
    suspend fun clearTrash()
    suspend fun restoreFromTrash(trashReminder: TrashReminder): Reminder
}

class OfflineReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val trashReminderDao: TrashReminderDao // Add the TrashReminderDao
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

    override suspend fun deleteReminder(reminder: Reminder) {
        // Move reminder to trash instead of deleting permanently
        val trashReminder = TrashReminder(
            id = 0, // Let Room auto-generate the ID
            title = reminder.title,
            notes = reminder.notes,
            dueDate = reminder.dueDate,
            isCompleted = reminder.isCompleted,
            deletedAt = System.currentTimeMillis(),
            priority = reminder.priority
        )
        insertTrashReminder(trashReminder)
        // Delete from main reminders
        reminderDao.delete(reminder)
    }

    override fun getCompletedRemindersStream(): Flow<List<Reminder>> =
        reminderDao.getCompletedReminders()

    // --- Added implementations ---
    override suspend fun getFutureIncompleteReminders(): List<Reminder> {
        return reminderDao.getFutureIncompleteReminders(System.currentTimeMillis())
    }

    override suspend fun getAllReminders(): List<Reminder> {
        return reminderDao.getAllReminders()
    }

    override suspend fun replaceAllReminders(reminders: List<Reminder>) {
        // Reset IDs to 0 so Room will auto-generate new IDs
        val remindersWithResetIds = reminders.map { it.copy(id = 0) }
        reminderDao.deleteAllReminders()
        reminderDao.insertAll(remindersWithResetIds)
    }

    override suspend fun appendReminders(reminders: List<Reminder>) {
        // Get all existing reminders to check for duplicates
        val existingReminders = getAllReminders()
        
        // Filter out reminders that already exist (based on all properties)
        val remindersToAdd = reminders.filter { newReminder ->
            // Check if an identical reminder already exists in the database
            !existingReminders.any { existingReminder ->
                newReminder.title == existingReminder.title &&
                newReminder.notes == existingReminder.notes &&
                newReminder.dueDate == existingReminder.dueDate &&
                newReminder.isCompleted == existingReminder.isCompleted &&
                newReminder.isDeleted == existingReminder.isDeleted &&
                newReminder.priority == existingReminder.priority
            }
        }
        
        // Add only the reminders that don't already exist
        if (remindersToAdd.isNotEmpty()) {
            // Reset IDs to 0 so Room will auto-generate new IDs for the new entries
            val remindersWithResetIds = remindersToAdd.map { it.copy(id = 0) }
            reminderDao.insertAll(remindersWithResetIds)
        }
    }

    override fun searchActiveReminders(query: String): Flow<List<Reminder>> {
        return if (query.isEmpty()) {
            // If query is empty, return all active (incomplete) reminders
            reminderDao.getIncompleteReminders()
        } else {
            // Otherwise, search for reminders matching the query
            reminderDao.searchActiveReminders(query)
        }
    }

    override fun searchCompletedReminders(query: String): Flow<List<Reminder>> {
        return if (query.isEmpty()) {
            // If query is empty, return all completed reminders
            reminderDao.getCompletedReminders()
        } else {
            // Otherwise, search for completed reminders matching the query
            reminderDao.searchCompletedReminders(query)
        }
    }

    override fun searchTrashReminders(query: String): Flow<List<TrashReminder>> {
        return if (query.isEmpty()) {
            // If query is empty, return all trash reminders
            trashReminderDao.getAllTrashReminders()
        } else {
            // Otherwise, search for trash reminders matching the query
            trashReminderDao.searchTrashReminders(query)
        }
    }

    // --- Trash-related implementations ---
    override fun getAllTrashRemindersStream(): Flow<List<TrashReminder>> =
        trashReminderDao.getAllTrashReminders()

    override suspend fun insertTrashReminder(trashReminder: TrashReminder) =
        trashReminderDao.insert(trashReminder)

    override suspend fun updateTrashReminder(trashReminder: TrashReminder) =
        trashReminderDao.update(trashReminder)

    override suspend fun deleteTrashReminderById(id: Int) =
        trashReminderDao.permanentlyDeleteById(id)

    override suspend fun clearTrash() =
        trashReminderDao.clearTrash()

    override suspend fun restoreFromTrash(trashReminder: TrashReminder): Reminder {
        // Create a Reminder from the TrashReminder
        val reminder = Reminder(
            id = 0, // Let Room auto-generate the ID
            title = trashReminder.title,
            notes = trashReminder.notes,
            dueDate = trashReminder.dueDate,
            isCompleted = trashReminder.isCompleted,
            isDeleted = false, // No longer deleted
            priority = trashReminder.priority
        )
        
        // Insert the reminder back to main table
        val newId = insertReminder(reminder)
        
        // Remove from trash
        trashReminderDao.permanentlyDeleteById(trashReminder.id)
        
        // Return updated reminder with new ID
        return reminder.copy(id = newId.toInt())
    }
}