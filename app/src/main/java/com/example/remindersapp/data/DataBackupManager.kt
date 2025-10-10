package com.example.remindersapp.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataBackupManager @Inject constructor(
    private val repository: ReminderRepository
) {
    private val json = Json { prettyPrint = true }

    /**
     * Get all reminders from the database and serialize them to a JSON string.
     * Runs on the IO dispatcher to avoid blocking the main thread.
     */
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val reminders = repository.getAllReminders()
        json.encodeToString(reminders)
    }

    /**
     * Deserialize a list of reminders from a JSON string and append to existing data.
     * Runs on the IO dispatcher.
     * @return Number of successfully imported reminders (after removing duplicates).
     * @throws Exception If the JSON format is invalid or other errors occur.
     */
    suspend fun importDataFromJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        val reminders = json.decodeFromString<List<Reminder>>(jsonString)
        val existingRemindersBefore = repository.getAllReminders()
        
        repository.appendReminders(reminders)
        
        val existingRemindersAfter = repository.getAllReminders()
        
        // Calculate how many reminders were actually added after deduplication
        return@withContext existingRemindersAfter.size - existingRemindersBefore.size
    }
}