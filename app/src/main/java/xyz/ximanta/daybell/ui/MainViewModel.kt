package xyz.ximanta.daybell.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.ximanta.daybell.data.local.Category
import xyz.ximanta.daybell.data.local.CategoryDao
import xyz.ximanta.daybell.data.local.Task
import xyz.ximanta.daybell.data.preferences.SettingsDataStore
import xyz.ximanta.daybell.data.preferences.UserPreferences
import xyz.ximanta.daybell.data.repository.TaskRepository
import java.util.Calendar

class MainViewModel(
    private val repository: TaskRepository,
    private val settingsDataStore: SettingsDataStore,
    private val categoryDao: CategoryDao
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

    val allCategories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayTasks: StateFlow<List<Task>> = activeTasks.map { tasks ->
        val startOfDay = getStartOfToday()
        val endOfDay = getEndOfToday()
        tasks.filter { it.dateTime in startOfDay..endOfDay }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val upcomingTasks: StateFlow<List<Task>> = activeTasks.map { tasks ->
        val endOfDay = getEndOfToday()
        tasks.filter { it.dateTime > endOfDay }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
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

    // --- Settings Updates ---
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

    // FIXED: Added missing bridge functions for SettingsScreen
    fun updateAutoRolloverOverdue(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateAutoRolloverOverdue(enabled)
        }
    }

    fun updateDefaultReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsDataStore.updateDefaultReminderTime(hour, minute)
        }
    }

    fun updateDefaultAlertSound(soundUri: String) {
        viewModelScope.launch {
            settingsDataStore.updateDefaultAlertSound(soundUri)
        }
    }

    // --- Category Management ---
    fun addCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            categoryDao.insertCategory(Category(name = name, colorHex = colorHex))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryDao.deleteCategory(category)
        }
    }
}
