package xyz.ximanta.daybell.data.repository

import kotlinx.coroutines.flow.Flow
import xyz.ximanta.daybell.data.local.Reminder
import xyz.ximanta.daybell.data.local.ReminderDao
import xyz.ximanta.daybell.data.local.Task
import xyz.ximanta.daybell.data.local.TaskDao
import xyz.ximanta.daybell.reminders.AlarmScheduler
import java.util.Calendar

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
            val reminderWithId = reminder.copy(taskId = taskId, id = 0)
            val reminderId = reminderDao.insertReminder(reminderWithId)
            
            if (isTaskActive(task)) {
                alarmScheduler.schedule(task.copy(id = taskId), reminderWithId.copy(id = reminderId))
            }
        }
        return taskId
    }

    /**
     * Updates an existing task and reschedules its reminders.
     */
    suspend fun updateTaskWithReminders(task: Task, reminders: List<Reminder>) {
        // 1. Cancel all existing alarms to prevent duplicates
        val existingReminders = reminderDao.getRemindersForTaskSync(task.id)
        for (existing in existingReminders) {
            alarmScheduler.cancel(existing.id)
        }

        // 2. Update DB
        taskDao.updateTask(task)
        reminderDao.deleteRemindersForTask(task.id)

        // 3. Insert new reminders and schedule
        for (reminder in reminders) {
            val reminderWithId = reminder.copy(taskId = task.id, id = 0)
            val reminderId = reminderDao.insertReminder(reminderWithId)
            
            // Only schedule if task is active
            if (isTaskActive(task)) {
                alarmScheduler.schedule(task, reminderWithId.copy(id = reminderId))
            }
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
            for (reminder in reminders) {
                alarmScheduler.cancel(reminder.id)
            }
        } else {
            for (reminder in reminders) {
                if (reminder.triggerTime > System.currentTimeMillis()) {
                    alarmScheduler.schedule(task, reminder)
                }
            }
        }
    }

    /**
     * Snoozes a reminder. 
     * Crucially, this does NOT overwrite the base reminder's triggerTime in the DB,
     * preventing recurring tasks from drifting off schedule.
     */
    suspend fun snoozeReminder(taskId: Long, reminderId: Long, minutes: Int) {
        val task = taskDao.getTaskByIdSync(taskId) ?: return
        val reminders = reminderDao.getRemindersForTaskSync(taskId)
        val reminder = reminders.find { it.id == reminderId } ?: return

        val snoozeTriggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        
        // Schedule a temporary, one-off alarm without touching the DB schedule
        alarmScheduler.scheduleSnooze(task, reminder, snoozeTriggerTime)
    }

    /**
     * Soft deletes a task and cancels its alarms.
     */
    suspend fun softDeleteTask(taskId: Long) {
        taskDao.softDeleteTask(taskId)
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
        if (!isTaskActive(task)) return

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

    /**
     * Reschedules ALL active alarms. Used after Backup Import or Device Reboot.
     */
    suspend fun rescheduleAllAlarms() {
        val activeTasks = taskDao.getUpcomingTasksForReschedule()
        for (task in activeTasks) {
            val reminders = reminderDao.getRemindersForTaskSync(task.id)
            for (reminder in reminders) {
                if (reminder.triggerTime > System.currentTimeMillis()) {
                    alarmScheduler.schedule(task, reminder)
                }
            }
        }
    }

    /**
     * Auto-Rollover: Moves overdue tasks to the current day (preserving their time)
     * and reschedules their alarms.
     */
    suspend fun rolloverOverdueTasks() {
        val now = System.currentTimeMillis()
        val activeTasks = taskDao.getUpcomingTasksForReschedule() // Gets all uncompleted, undeleted tasks
        
        for (task in activeTasks) {
            if (task.dateTime < now) {
                val originalCal = Calendar.getInstance().apply { timeInMillis = task.dateTime }
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, originalCal.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, originalCal.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                }
                
                // If the new time is still in the past (e.g., 8 AM task rolled over at 10 AM), push to tomorrow
                if (newCal.timeInMillis < now) {
                    newCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                val updatedTask = task.copy(dateTime = newCal.timeInMillis)
                taskDao.updateTask(updatedTask)

                // Reschedule reminders for this task
                val reminders = reminderDao.getRemindersForTaskSync(task.id)
                for (reminder in reminders) {
                    alarmScheduler.cancel(reminder.id)
                    val updatedReminder = reminder.copy(triggerTime = newCal.timeInMillis)
                    reminderDao.updateReminder(updatedReminder)
                    alarmScheduler.schedule(updatedTask, updatedReminder)
                }
            }
        }
    }

    private fun isTaskActive(task: Task): Boolean {
        return !task.isCompleted && !task.isDeleted
    }
}
