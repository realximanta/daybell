package xyz.ximanta.daybell.data.repository

import kotlinx.coroutines.flow.Flow
import xyz.ximanta.daybell.data.local.Reminder
import xyz.ximanta.daybell.data.local.ReminderDao
import xyz.ximanta.daybell.data.local.Task
import xyz.ximanta.daybell.data.local.TaskDao
import xyz.ximanta.daybell.reminders.AlarmScheduler

class TaskRepository(
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val alarmScheduler: AlarmScheduler
) {

    val allActiveTasks: Flow<List<Task>> = taskDao.getAllActiveTasks()
    val completedTasks: Flow<List<Task>> = taskDao.getCompletedTasks()
    val deletedTasks: Flow<List<Task>> = taskDao.getDeletedTasks()

    fun getTasksForDay(startOfDay: Long, endOfDay: Long): Flow<List<Task>> {
        return taskDao.getTasksForDay(startOfDay, endOfDay)
    }

    fun getTaskById(taskId: Long): Flow<Task?> {
        return taskDao.getTaskById(taskId)
    }

    suspend fun getTaskByIdSync(taskId: Long): Task? {
        return taskDao.getTaskByIdSync(taskId)
    }

    fun getRemindersForTask(taskId: Long): Flow<List<Reminder>> {
        return reminderDao.getRemindersForTask(taskId)
    }

    /**
     * Inserts a new task and schedules its reminders.
     */
    suspend fun insertTaskWithReminders(task: Task, reminders: List<Reminder>): Long {
        val taskId = taskDao.insertTask(task)
        
        for (reminder in reminders) {
            val reminderWithId = reminder.copy(taskId = taskId)
            val reminderId = reminderDao.insertReminder(reminderWithId)
            
            // Schedule the alarm using the newly generated IDs
            alarmScheduler.schedule(
                task.copy(id = taskId), 
                reminderWithId.copy(id = reminderId)
            )
        }
        return taskId
    }

    /**
     * Updates an existing task and reschedules its reminders.
     * Cancels old alarms to prevent duplicates.
     */
    suspend fun updateTaskWithReminders(task: Task, reminders: List<Reminder>) {
        taskDao.updateTask(task)
        
        // 1. Cancel all existing alarms for this task
        val existingReminders = reminderDao.getRemindersForTaskSync(task.id)
        for (existing in existingReminders) {
            alarmScheduler.cancel(existing.id)
        }

        // 2. Delete old reminders from DB
        reminderDao.deleteRemindersForTask(task.id)

        // 3. Insert new reminders and schedule new alarms
        for (reminder in reminders) {
            val reminderWithId = reminder.copy(taskId = task.id, id = 0) // Reset ID to auto-generate new
            val reminderId = reminderDao.insertReminder(reminderWithId)
            
            alarmScheduler.schedule(
                task, 
                reminderWithId.copy(id = reminderId)
            )
        }
    }

    /**
     * Toggles task completion. Cancels alarms if completed, reschedules if un-completed.
     */
    suspend fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean) {
        taskDao.setTaskCompletion(taskId, isCompleted)
        val task = taskDao.getTaskByIdSync(taskId) ?: return
        val reminders = reminderDao.getRemindersForTaskSync(taskId)

        if (isCompleted) {
            // Cancel all alarms
            for (reminder in reminders) {
                alarmScheduler.cancel(reminder.id)
            }
        } else {
            // Reschedule alarms if the task is un-completed
            for (reminder in reminders) {
                if (reminder.triggerTime > System.currentTimeMillis()) {
                    alarmScheduler.schedule(task, reminder)
                }
            }
        }
    }

    /**
     * Soft deletes a task and cancels its alarms.
     */
    suspend fun softDeleteTask(taskId: Long) {
        taskDao.softDeleteTask(taskId)
        
        // Cancel alarms so they don't fire for a deleted task
        val reminders = reminderDao.getRemindersForTaskSync(taskId)
        for (reminder in reminders) {
            alarmScheduler.cancel(reminder.id)
        }
    }

    /**
     * Restores a soft-deleted task and reschedules its future alarms.
     */
    suspend fun restoreTask(taskId: Long) {
        taskDao.restoreTask(taskId)
        
        val task = taskDao.getTaskByIdSync(taskId) ?: return
        if (task.isCompleted) return // Don't schedule if it was already completed

        val reminders = reminderDao.getRemindersForTaskSync(taskId)
        for (reminder in reminders) {
            if (reminder.triggerTime > System.currentTimeMillis()) {
                alarmScheduler.schedule(task, reminder)
            }
        }
    }

    /**
     * Permanently deletes a task from the database.
     */
    suspend fun permanentlyDeleteTask(task: Task) {
        val reminders = reminderDao.getRemindersForTaskSync(task.id)
        for (reminder in reminders) {
            alarmScheduler.cancel(reminder.id)
        }
        taskDao.deleteTask(task)
    }
}
