package xyz.ximanta.daybell.ui.screens

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.ximanta.daybell.ui.MainViewModel
import xyz.ximanta.daybell.ui.theme.NeonTeal
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showWeekStartDialog by remember { mutableStateOf(false) }
    var showDefaultTimeDialog by remember { mutableStateOf(false) }
    var showAlertSoundDialog by remember { mutableStateOf(false) }

    // FIXED: File pickers for Backup & Restore
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportData(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importData(it) }
    }

    // Permission Checks
    val hasNotificationPermission = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val hasExactAlarmPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.canScheduleExactAlarms()
    } else true
    val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- HEALTH & PERMISSIONS SECTION ---
            SettingsSection(title = "App Health & Permissions") {
                PermissionRow(
                    title = "Notifications",
                    description = "Required to show reminders",
                    isGranted = hasNotificationPermission,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PermissionRow(
                    title = "Exact Alarms",
                    description = "Required for precise timing (Android 12+)",
                    isGranted = hasExactAlarmPermission,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PermissionRow(
                    title = "Battery Optimization",
                    description = "Disable to ensure reminders fire on time",
                    isGranted = isIgnoringBatteryOptimizations,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // --- APPEARANCE SECTION ---
            SettingsSection(title = "Appearance") {
                SettingsRow(
                    title = "Theme",
                    subtitle = prefs.themeMode.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = { showThemeDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSwitchRow(
                    title = "24-Hour Time",
                    subtitle = "Use 14:00 instead of 2:00 PM",
                    checked = prefs.use24HourFormat,
                    onCheckedChange = { viewModel.update24HourFormat(it) }
                )
            }

            // --- REMINDERS SECTION ---
            SettingsSection(title = "Reminders") {
                SettingsRow(
                    title = "Default Reminder Time",
                    subtitle = String.format("%02d:%02d", prefs.defaultReminderHour, prefs.defaultReminderMinute),
                    onClick = { showDefaultTimeDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsRow(
                    title = "Default Alert Sound",
                    subtitle = prefs.defaultAlertSound,
                    onClick = { showAlertSoundDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsRow(
                    title = "Default Snooze Time",
                    subtitle = "${prefs.snoozeDuration} minutes",
                    onClick = { showSnoozeDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsRow(
                    title = "Start of Week",
                    subtitle = if (prefs.weekStartDay == 1) "Monday" else "Sunday",
                    onClick = { showWeekStartDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSwitchRow(
                    title = "Auto-Rollover Overdue",
                    subtitle = "Move missed tasks to today",
                    checked = prefs.autoRolloverOverdue,
                    onCheckedChange = { viewModel.updateAutoRolloverOverdue(it) }
                )
            }

            // --- DATA & BACKUP SECTION (FIXED: Added Export/Import UI) ---
            SettingsSection(title = "Data & Backup") {
                Text(
                    text = "Export your tasks and categories to a local file, or restore them from a previous backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { exportLauncher.launch("daybell_backup.json") }
                    ) {
                        Text("Export Data")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { importLauncher.launch(arrayOf("application/json")) }
                    ) {
                        Text("Import Data")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialogs
    if (showThemeDialog) {
        SelectionDialog(
            title = "Choose Theme",
            options = listOf("SYSTEM", "LIGHT", "DARK"),
            selectedOption = prefs.themeMode,
            onOptionSelected = { viewModel.updateThemeMode(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showSnoozeDialog) {
        val snoozeOptions = listOf(5, 10, 15, 20, 30)
        SelectionDialog(
            title = "Default Snooze Time",
            options = snoozeOptions.map { "$it minutes" },
            selectedOption = "${prefs.snoozeDuration} minutes",
            onOptionSelected = {
                viewModel.updateSnoozeDuration(it.replace(" minutes", "").toInt())
                showSnoozeDialog = false
            },
            onDismiss = { showSnoozeDialog = false }
        )
    }

    if (showWeekStartDialog) {
        SelectionDialog(
            title = "Start of Week",
            options = listOf("Monday", "Sunday"),
            selectedOption = if (prefs.weekStartDay == 1) "Monday" else "Sunday",
            onOptionSelected = {
                viewModel.updateWeekStartDay(if (it == "Monday") 1 else 7)
                showWeekStartDialog = false
            },
            onDismiss = { showWeekStartDialog = false }
        )
    }

    if (showAlertSoundDialog) {
        SelectionDialog(
            title = "Default Alert Sound",
            options = listOf("default", "silent", "vibrate"),
            selectedOption = prefs.defaultAlertSound,
            onOptionSelected = { viewModel.updateDefaultAlertSound(it); showAlertSoundDialog = false },
            onDismiss = { showAlertSoundDialog = false }
        )
    }

    if (showDefaultTimeDialog) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, prefs.defaultReminderHour)
            set(Calendar.MINUTE, prefs.defaultReminderMinute)
        }
        TimePickerDialog(
            context,
            { _, hour, minute ->
                viewModel.updateDefaultReminderTime(hour, minute)
                showDefaultTimeDialog = false
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            prefs.use24HourFormat
        ).show()
        showDefaultTimeDialog = false
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PermissionRow(title: String, description: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = if (isGranted) "Granted" else "Denied",
            tint = if (isGranted) NeonTeal else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = { onOptionSelected(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = option)
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
