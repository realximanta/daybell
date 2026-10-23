package xyz.ximanta.daybell.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.ximanta.daybell.data.local.Task
import xyz.ximanta.daybell.data.preferences.SettingsDataStore
import xyz.ximanta.daybell.data.preferences.UserPreferences
import xyz.ximanta.daybell.data.repository.TaskRepository

class MainViewModel(
    private val repository: TaskRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = settingsDataStore.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val activeTasks: StateFlow<List<Task>> = repository.allActiveTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completedTasks: StateFlow<List<Task>> = repository.completedTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedTasks: StateFlow<List<Task>> = repository.deletedTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Check auto-rollover setting on launch
        viewModelScope.launch {
            userPreferences.collect { prefs ->
                if (prefs.autoRolloverOverdue) {
                    repository.rolloverOverdueTasks()
                }
            }
        }
    }

    fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId, isCompleted)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.softDeleteTask(taskId)
        }
    }

    fun restoreTask(taskId: Long) {
        viewModelScope.launch {
            repository.restoreTask(taskId)
        }
    }

    fun permanentlyDeleteTask(task: Task) {
        viewModelScope.launch {
            repository.permanentlyDeleteTask(task)
        }
    }

    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            settingsDataStore.updateThemeMode(themeMode)
        }
    }

    fun update24HourFormat(use24Hour: Boolean) {
        viewModelScope.launch {
            settingsDataStore.update24HourFormat(use24Hour)
        }
    }

    fun updateSnoozeDuration(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.updateSnoozeDuration(minutes)
        }
    }

    fun updateWeekStartDay(day: Int) {
        viewModelScope.launch {
            settingsDataStore.updateWeekStartDay(day)
        }
    }

    fun updateAutoRolloverOverdue(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAutoRolloverOverdue(enabled)
            if (enabled) {
                repository.rolloverOverdueTasks()
            }
        }
    }

    /**
     * Call this after a successful backup import to re-sync all alarms.
     */
    fun onBackupRestored() {
        viewModelScope.launch {
            repository.rescheduleAllAlarms()
        }
    }
}
