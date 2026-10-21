package xyz.ximanta.daybell.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.ximanta.daybell.data.local.Category
import xyz.ximanta.daybell.data.local.CategoryDao
import xyz.ximanta.daybell.data.local.Reminder
import xyz.ximanta.daybell.data.local.Task
import xyz.ximanta.daybell.data.preferences.SettingsDataStore
import xyz.ximanta.daybell.data.preferences.UserPreferences
import xyz.ximanta.daybell.data.repository.TaskRepository

data class TaskEditorState(
    val taskId: Long = 0,
    val title: String = "",
    val description: String = "",
    val dateTime: Long = System.currentTimeMillis(),
    val category: String = "General",
    val priority: Int = 2,
    val repeatType: String = "NONE",
    val repeatDays: String = "",
    val advanceAlertMinutes: Int = 0,
    val snoozeDurationMinutes: Int = 10,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false
)

class TaskEditorViewModel(
    private val repository: TaskRepository,
    private val categoryDao: CategoryDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditorState())
    val state: StateFlow<TaskEditorState> = _state.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryDao.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userPreferences: StateFlow<UserPreferences> = settingsDataStore.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private var existingReminders: List<Reminder> = emptyList()

    fun loadTask(taskId: Long?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            if (taskId != null && taskId > 0) {
                // Editing mode
                val task = repository.getTaskByIdSync(taskId)
                val reminders = repository.getRemindersForTask(taskId).first()
                existingReminders = reminders
                val primaryReminder = reminders.firstOrNull()

                if (task != null) {
                    _state.value = TaskEditorState(
                        taskId = task.id,
                        title = task.title,
                        description = task.description ?: "",
                        dateTime = task.dateTime,
                        category = task.category,
                        priority = task.priority,
                        repeatType = primaryReminder?.repeatType ?: "NONE",
                        repeatDays = primaryReminder?.repeatDays ?: "",
                        advanceAlertMinutes = primaryReminder?.advanceAlertMinutes ?: 0,
                        snoozeDurationMinutes = primaryReminder?.snoozeDurationMinutes
                            ?: settingsDataStore.userPreferencesFlow.first().snoozeDuration,
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(isLoading = false)
                }
            } else {
                // New task mode
                existingReminders = emptyList()
                
                val prefs = settingsDataStore.userPreferencesFlow.first()
                val defaultDateTime = getDefaultReminderDateTime(prefs.defaultReminderHour, prefs.defaultReminderMinute)

                _state.value = TaskEditorState(
                    dateTime = defaultDateTime,
                    snoozeDurationMinutes = prefs.snoozeDuration,
                    isLoading = false
                )
            }
        }
    }

    private fun getDefaultReminderDateTime(hour: Int, minute: Int): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
        cal.set(java.util.Calendar.MINUTE, minute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun updateTitle(title: String) {
        _state.value = _state.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _state.value = _state.value.copy(description = description)
    }

    fun updateDateTime(dateTime: Long) {
        _state.value = _state.value.copy(dateTime = dateTime)
    }

    fun updateCategory(category: String) {
        _state.value = _state.value.copy(category = category)
    }

    fun updatePriority(priority: Int) {
        _state.value = _state.value.copy(priority = priority)
    }

    fun updateRepeatType(repeatType: String) {
        _state.value = _state.value.copy(repeatType = repeatType)
    }

    fun updateRepeatDays(repeatDays: String) {
        _state.value = _state.value.copy(repeatDays = repeatDays)
    }

    fun updateAdvanceAlert(minutes: Int) {
        _state.value = _state.value.copy(advanceAlertMinutes = minutes)
    }

    fun updateSnoozeDuration(minutes: Int) {
        _state.value = _state.value.copy(snoozeDurationMinutes = minutes)
    }

    fun saveTask(onSuccess: () -> Unit) {
        val currentState = _state.value
        if (currentState.title.isBlank()) return

        viewModelScope.launch {
            _state.value = currentState.copy(isSaving = true)

            // FIXED: Fetch existing task to preserve its completion state and creation date
            val existingTask = if (currentState.taskId > 0) repository.getTaskByIdSync(currentState.taskId) else null

            val task = Task(
                id = currentState.taskId,
                title = currentState.title.trim(),
                description = currentState.description.trim().ifBlank { null },
                dateTime = currentState.dateTime,
                category = currentState.category,
                priority = currentState.priority,
                isCompleted = existingTask?.isCompleted ?: false,
                createdAt = existingTask?.createdAt ?: System.currentTimeMillis()
            )

            val reminder = Reminder(
                id = 0,
                taskId = task.id,
                triggerTime = currentState.dateTime,
                repeatType = currentState.repeatType,
                repeatDays = currentState.repeatDays.ifBlank { null },
                advanceAlertMinutes = currentState.advanceAlertMinutes,
                snoozeDurationMinutes = currentState.snoozeDurationMinutes
            )

            try {
                if (currentState.taskId > 0) {
                    repository.updateTaskWithReminders(task, listOf(reminder))
                } else {
                    repository.insertTaskWithReminders(task, listOf(reminder))
                }
                _state.value = _state.value.copy(isSaving = false)
                onSuccess()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }
}
