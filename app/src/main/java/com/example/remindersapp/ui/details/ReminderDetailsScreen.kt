package com.example.remindersapp.ui.details

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.collectLatest


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReminderDetailsScreen(
    onNavigateUp: () -> Unit,
    viewModel: ReminderDetailsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState
    val context = LocalContext.current

    // Android 13+ 的通知权限处理
    val postNotificationPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
        } else {
            null
        }

    // Date and Time Picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.dueDate)
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.dueDate?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.HOUR_OF_DAY) } ?: 0,
        initialMinute = uiState.dueDate?.let { Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MINUTE) } ?: 0,
        is24Hour = true
    )

    // --- DatePickerDialog ---
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    // 将选择的日期更新到 ViewModel
                    datePickerState.selectedDateMillis?.let {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = it
                        // 保持时间部分不变，只更新日期
                        val currentDueDate = Calendar.getInstance()
                        currentDueDate.timeInMillis = uiState.dueDate ?: System.currentTimeMillis()
                        currentDueDate.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                        viewModel.updateDueDate(currentDueDate.timeInMillis)
                    }
                    showTimePicker = true // 选择日期后接着显示时间选择器
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- TimePickerDialog ---
    if (showTimePicker) {
        // 这是正确实现的 TimePickerDialog
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            modifier = Modifier.wrapContentSize(),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        val currentDueDate = Calendar.getInstance()
                        currentDueDate.timeInMillis = uiState.dueDate ?: System.currentTimeMillis()
                        currentDueDate.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        currentDueDate.set(Calendar.MINUTE, timePickerState.minute)
                        currentDueDate.set(Calendar.SECOND, 0)
                        viewModel.updateDueDate(currentDueDate.timeInMillis)
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            }
        )
    }

    // --- 新增代码：监听一次性事件 ---
    LaunchedEffect(key1 = true) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DetailsScreenEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "新建提醒" else "编辑提醒") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        // 这个 Icon 现在使用的是新的 AutoMirrored 版本
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {

                viewModel.saveReminder()

                if (uiState.dueDate != null && (uiState.dueDate ?: 0) > System.currentTimeMillis()) {
                    if (postNotificationPermission == null || postNotificationPermission.status.isGranted) {
                        viewModel.saveReminder()
                        onNavigateUp()
                    } else {
                        postNotificationPermission.launchPermissionRequest()
                        Toast.makeText(context, "需要通知权限才能设置提醒", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.saveReminder()
                    onNavigateUp()
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
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("备注") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 日期时间显示和设置区域
            Text("设置提醒时间", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = uiState.dueDate?.let { formatDateTime(it) } ?: "未设置",
                    style = MaterialTheme.typography.bodyLarge
                )
                if (uiState.dueDate != null) {
                    IconButton(onClick = { viewModel.updateDueDate(null) }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除时间")
                    }
                }
            }
        }
    }
}

private fun formatDateTime(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}