package com.example.remindersapp.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remindersapp.data.DataBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import javax.inject.Inject

sealed class SettingsScreenEvent {
    data class ShowToast(val message: String) : SettingsScreenEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataBackupManager: DataBackupManager
) : ViewModel() {

    private val _events = MutableSharedFlow<SettingsScreenEvent>()
    val events = _events.asSharedFlow()

    fun onExportData(uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = dataBackupManager.exportDataToJson()
                context.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).use { fos ->
                        fos.write(jsonString.toByteArray())
                    }
                }
                _events.emit(SettingsScreenEvent.ShowToast("Data exported successfully"))
            } catch (e: Exception) {
                _events.emit(SettingsScreenEvent.ShowToast("Export failed: ${e.message}"))
            }
        }
    }

    fun onImportData(uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonString = readTextFromUri(uri)
                val count = dataBackupManager.importDataFromJson(jsonString)
                _events.emit(SettingsScreenEvent.ShowToast("Successfully imported $count reminders"))
            } catch (e: Exception) {
                _events.emit(SettingsScreenEvent.ShowToast("Import failed: ${e.message}"))
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }
}