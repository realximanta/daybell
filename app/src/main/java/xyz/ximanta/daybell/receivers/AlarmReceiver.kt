package xyz.ximanta.daybell.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.ximanta.daybell.data.local.AppDatabase
import xyz.ximanta.daybell.data.repository.TaskRepository
import xyz.ximanta.daybell.reminders.AlarmScheduler
import xyz.ximanta.daybell.reminders.NotificationHelper
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val reminderId = intent.getLongExtra("EXTRA_REMINDER_ID", -1L)
        val isSnooze = intent.getBooleanExtra("EXTRA_IS_SNOOZE", false)

        if (taskId == -1L || reminderId == -1L) return

        val appContext = context.applicationContext
        val db = AppDatabase.getDatabase(appContext)
        val notificationHelper = NotificationHelper(appContext)
        val alarmScheduler = AlarmScheduler(appContext)
        val repository = TaskRepository(db.taskDao(), db.reminderDao(), alarmScheduler)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_DONE -> {
                        // Uses repository which safely marks complete and cancels ALL future alarms
                        repository.toggleTaskCompletion(taskId, true)
                        notificationHelper.cancelNotification(reminderId)
                    }

                    ACTION_SNOOZE -> {
                        val snoozeDuration = intent.getIntExtra("EXTRA_SNOOZE_DURATION", 10)
                        notificationHelper.cancelNotification(reminderId)
                        
                        // Uses robust snooze logic that doesn't corrupt the DB schedule
                        repository.snoozeReminder(taskId, reminderId, snoozeDuration)
                    }

                    else -> {
                        // Default Action: Alarm Fired!
                        val title = intent.getStringExtra("EXTRA_TASK_TITLE") ?: "Task Reminder"
                        val description = intent.getStringExtra("EXTRA_TASK_DESC")
                        val repeatType = intent.getStringExtra("EXTRA_REPEAT_TYPE") ?: "NONE"
                        val snoozeDuration = intent.getIntExtra("EXTRA_SNOOZE_DURATION", 10)

                        // 1. Show the notification
                        notificationHelper.showNotification(
                            taskId = taskId,
                            reminderId = reminderId,
                            title = title,
                            description = description,
                            repeatType = repeatType,
                            snoozeDuration = snoozeDuration
                        )

                        // 2. Handle Repeating Tasks (Chain method)
                        // Snoozes do not repeat, so we only calculate the next occurrence if it's a base alarm
                        if (repeatType != "NONE" && !isSnooze) {
                            val reminders = db.reminderDao().getRemindersForTaskSync(taskId)
                            val reminder = reminders.find { it.id == reminderId }
                            val task = db.taskDao().getTaskByIdSync(taskId)

                            if (reminder != null && task != null) {
                                val nextTriggerTime = calculateNextOccurrence(reminder.triggerTime, repeatType, reminder.repeatDays)
                                if (nextTriggerTime > 0) {
                                    val nextReminder = reminder.copy(triggerTime = nextTriggerTime)
                                    db.reminderDao().updateReminder(nextReminder)
                                    
                                    // Update the task's dateTime so the dashboard doesn't falsely show it as overdue
                                    val updatedTask = task.copy(dateTime = nextTriggerTime)
                                    db.taskDao().updateTask(updatedTask)

                                    alarmScheduler.schedule(updatedTask, nextReminder)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error processing alarm action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Calculates the next trigger time based on the repeat type, including custom days.
     */
    private fun calculateNextOccurrence(currentTriggerTime: Long, repeatType: String, repeatDays: String?): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTriggerTime
        }

        when (repeatType) {
            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            "CUSTOM" -> {
                if (repeatDays.isNullOrBlank()) return -1L // Safety check
                val daysList = repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (daysList.isEmpty()) return -1L

                // Loop forward day by day until we hit a selected day
                do {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                } while (!daysList.contains(calendar.get(Calendar.DAY_OF_WEEK)))
                
                return calendar.timeInMillis
            }
            else -> return -1L
        }
        
        // If the next occurrence is somehow in the past (e.g. device was off for days), 
        // keep adding intervals until it's in the future.
        var nextTime = calendar.timeInMillis
        val now = System.currentTimeMillis()
        while (nextTime <= now) {
             when (repeatType) {
                "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
                "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            }
            nextTime = calendar.timeInMillis
        }

        return nextTime
    }

    companion object {
        const val ACTION_DONE = "xyz.ximanta.daybell.ACTION_DONE"
        const val ACTION_SNOOZE = "xyz.ximanta.daybell.ACTION_SNOOZE"
    }
}
