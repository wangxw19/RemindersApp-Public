package com.example.remindersapp.ui.details

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.remindersapp.data.Priority
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReminderDetailsScreen(
    onNavigateUp: () -> Unit,
    viewModel: ReminderDetailsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    val postNotificationPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
        } else null

    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DetailsScreenEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is DetailsScreenEvent.NavigateUp -> {
                    onNavigateUp()
                }
            }
        }
    }

    val navigateBack = {
        if (viewModel.hasChanges) {
            showUnsavedChangesDialog = true
        } else {
            onNavigateUp()
        }
    }

    BackHandler(onBack = navigateBack)

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("未保存的更改") },
            text = { Text("您有未保存的更改，确定要放弃吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedChangesDialog = false
                    onNavigateUp()
                }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) { Text("取消") }
            }
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.dueDate)
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.dueDate?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) } ?: 0,
        initialMinute = uiState.dueDate?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE) } ?: 0,
        is24Hour = true
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    viewModel.onEvent(DetailsUIEvent.OnDateSelected(datePickerState.selectedDateMillis))
                    showTimePicker = true
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            modifier = Modifier.wrapContentSize(),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    viewModel.onEvent(DetailsUIEvent.OnTimeSelected(timePickerState.hour, timePickerState.minute))
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("取消") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "新建提醒" else "编辑提醒") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (uiState.dueDate != null && (uiState.dueDate ?: 0) > System.currentTimeMillis()) {
                    if (postNotificationPermission == null || postNotificationPermission.status.isGranted) {
                        viewModel.onEvent(DetailsUIEvent.OnSaveClick)
                    } else {
                        postNotificationPermission.launchPermissionRequest()
                    }
                } else {
                    viewModel.onEvent(DetailsUIEvent.OnSaveClick)
                }
            }) {
                Icon(Icons.Default.Check, contentDescription = "保存")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onEvent(DetailsUIEvent.OnTitleChange(it)) },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.title.isBlank() && viewModel.hasChanges
            )
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.onEvent(DetailsUIEvent.OnNotesChange(it)) },
                label = { Text("备注") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )

            Text("优先级", style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Priority.values().forEach { priority ->
                    SegmentedButton(
                        selected = uiState.priority == priority,
                        onClick = { viewModel.onEvent(DetailsUIEvent.OnPriorityChange(priority)) },
                        // --- 核心修正：移除多余参数，使用无参函数 ---
                        shape = SegmentedButtonDefaults.shape()
                    ) {
                        Text(priority.displayName)
                    }
                }
            }

            Text("设置提醒", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.dueDate != null,
                    onClick = {
                        viewModel.onEvent(DetailsUIEvent.OnSetDefaultTime)
                        showDatePicker = true
                    },
                    label = { Text(uiState.dueDate?.let { formatDate(it) } ?: "设置日期") }
                )

                if (uiState.dueDate != null) {
                    FilterChip(
                        selected = true,
                        onClick = { showTimePicker = true },
                        label = { Text(formatTime(uiState.dueDate!!)) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.onEvent(DetailsUIEvent.OnClearDate) }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "清除时间")
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(millis))
}

private fun formatTime(millis: Long): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}