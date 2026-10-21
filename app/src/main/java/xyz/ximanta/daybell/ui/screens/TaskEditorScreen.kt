package xyz.ximanta.daybell.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.ximanta.daybell.ui.TaskEditorViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    viewModel: TaskEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val timeFormat = if (userPreferences.use24HourFormat) {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    } else {
        SimpleDateFormat("h:mm a", Locale.getDefault())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.taskId > 0) "Edit Task" else "New Task",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Task Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = dateFormat.format(Date(state.dateTime)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Date")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = timeFormat.format(Date(state.dateTime)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time") },
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Pick Time")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Priority Dropdown (FIXED: Audit noted this was missing)
            var expandedPriority by remember { mutableStateOf(false) }
            val priorityOptions = listOf(1 to "Low", 2 to "Medium", 3 to "High")
            ExposedDropdownMenuBox(
                expanded = expandedPriority,
                onExpandedChange = { expandedPriority = !expandedPriority }
            ) {
                OutlinedTextField(
                    value = priorityOptions.find { it.first == state.priority }?.second ?: "Medium",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Priority") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPriority) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedPriority,
                    onDismissRequest = { expandedPriority = false }
                ) {
                    priorityOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.updatePriority(value)
                                expandedPriority = false
                            }
                        )
                    }
                }
            }

            // Category Dropdown
            var expandedCategory by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = state.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.updateCategory(category.name)
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            // Repeat Type Dropdown
            var expandedRepeat by remember { mutableStateOf(false) }
            val repeatOptions = listOf("NONE", "DAILY", "WEEKLY", "MONTHLY", "CUSTOM")
            ExposedDropdownMenuBox(
                expanded = expandedRepeat,
                onExpandedChange = { expandedRepeat = !expandedRepeat }
            ) {
                OutlinedTextField(
                    value = if (state.repeatType == "NONE") "Does not repeat" else state.repeatType.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Repeat") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRepeat) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedRepeat,
                    onDismissRequest = { expandedRepeat = false }
                ) {
                    repeatOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(if (option == "NONE") "Does not repeat" else option.lowercase()
                                    .replaceFirstChar { it.uppercase() })
                            },
                            onClick = {
                                viewModel.updateRepeatType(option)
                                expandedRepeat = false
                            }
                        )
                    }
                }
            }

            // Custom Repeat Days Checkboxes (FIXED: Audit noted this was missing)
            if (state.repeatType == "CUSTOM") {
                Text("Select Days", style = MaterialTheme.typography.labelLarge)
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val selectedDays = state.repeatDays.split(",").filter { it.isNotBlank() }
                
                Column {
                    days.chunked(4).forEach { rowDays ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowDays.forEach { day ->
                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedDays.contains(day),
                                        onCheckedChange = { isChecked ->
                                            val newDays = if (isChecked) {
                                                selectedDays + day
                                            } else {
                                                selectedDays - day
                                            }
                                            viewModel.updateRepeatDays(newDays.joinToString(","))
                                        }
                                    )
                                    Text(day)
                                }
                            }
                        }
                    }
                }
            }

            // Advance Alert Dropdown (FIXED: Audit noted this was missing)
            var expandedAdvance by remember { mutableStateOf(false) }
            val advanceOptions = listOf(0 to "At time of event", 5 to "5 min before", 10 to "10 min before", 15 to "15 min before", 30 to "30 min before")
            ExposedDropdownMenuBox(
                expanded = expandedAdvance,
                onExpandedChange = { expandedAdvance = !expandedAdvance }
            ) {
                OutlinedTextField(
                    value = advanceOptions.find { it.first == state.advanceAlertMinutes }?.second ?: "At time of event",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Advance Alert") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAdvance) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedAdvance,
                    onDismissRequest = { expandedAdvance = false }
                ) {
                    advanceOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.updateAdvanceAlert(value)
                                expandedAdvance = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.saveTask(onSuccess = onNavigateBack) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state.title.isNotBlank() && !state.isSaving
            ) {
                Text(
                    text = if (state.isSaving) "Saving..." else "Save Task",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.dateTime)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedDate
                            val existingTime = Calendar.getInstance().apply { timeInMillis = state.dateTime }
                            set(Calendar.HOUR_OF_DAY, existingTime.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, existingTime.get(Calendar.MINUTE))
                        }
                        viewModel.updateDateTime(cal.timeInMillis)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = state.dateTime }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = userPreferences.use24HourFormat
        )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { Column(modifier = Modifier.fillMaxWidth()) { TimePicker(state = timePickerState) } },
            confirmButton = {
                TextButton(onClick = {
                    val newCal = Calendar.getInstance().apply {
                        timeInMillis = state.dateTime
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }
                    viewModel.updateDateTime(newCal.timeInMillis)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}
