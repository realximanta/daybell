package xyz.ximanta.daybell.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.ximanta.daybell.data.local.AppDatabase
import xyz.ximanta.daybell.reminders.AlarmScheduler
import xyz.ximanta.daybell.reminders.NotificationHelper
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val reminderId = intent.getLongExtra("EXTRA_REMINDER_ID", -1L)

        if (taskId == -1L || reminderId == -1L) return

        val appContext = context.applicationContext
        val db = AppDatabase.getDatabase(appContext)
        val notificationHelper = NotificationHelper(appContext)
        val alarmScheduler = AlarmScheduler(appContext)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_DONE -> {
                        db.taskDao().setTaskCompletion(taskId, true)
                        notificationHelper.cancelNotification(reminderId)
                        alarmScheduler.cancel(reminderId)
                        
                        val allReminders = db.reminderDao().getRemindersForTaskSync(taskId)
                        for (r in allReminders) {
                            alarmScheduler.cancel(r.id)
                        }
                    }

                    ACTION_SNOOZE -> {
                        val snoozeDuration = intent.getIntExtra("EXTRA_SNOOZE_DURATION", 10)
                        notificationHelper.cancelNotification(reminderId)
                        
                        val reminders = db.reminderDao().getRemindersForTaskSync(taskId)
                        val reminder = reminders.find { it.id == reminderId }
                        val task = db.taskDao().getTaskByIdSync(taskId)

                        if (reminder != null && task != null) {
                            val newTriggerTime = System.currentTimeMillis() + (snoozeDuration * 60 * 1000L)
                            val snoozedReminder = reminder.copy(
                                id = reminder.id + 500000,
                                triggerTime = newTriggerTime
                            )
                            alarmScheduler.schedule(task, snoozedReminder)
                        }
                    }

                    else -> {
                        val title = intent.getStringExtra("EXTRA_TASK_TITLE") ?: "Task Reminder"
                        val description = intent.getStringExtra("EXTRA_TASK_DESC")
                        val repeatType = intent.getStringExtra("EXTRA_REPEAT_TYPE") ?: "NONE"
                        val snoozeDuration = intent.getIntExtra("EXTRA_SNOOZE_DURATION", 10)
                        val isAdvanceAlert = intent.getBooleanExtra("EXTRA_IS_ADVANCE_ALERT", false)

                        notificationHelper.showNotification(
                            taskId = taskId,
                            reminderId = reminderId,
                            title = title,
                            description = description,
                            repeatType = repeatType,
                            snoozeDuration = snoozeDuration,
                            isAdvanceAlert = isAdvanceAlert
                        )

                        if (repeatType != "NONE" && !isAdvanceAlert) {
                            val reminders = db.reminderDao().getRemindersForTaskSync(taskId)
                            val reminder = reminders.find { it.id == reminderId }
                            val task = db.taskDao().getTaskByIdSync(taskId)

                            if (reminder != null && task != null) {
                                // FIXED: Passed repeatDays to the calculator for CUSTOM support
                                val nextTriggerTime = calculateNextOccurrence(reminder.triggerTime, repeatType, reminder.repeatDays)
                                
                                if (nextTriggerTime > 0) {
                                    val updatedTask = task.copy(dateTime = nextTriggerTime)
                                    db.taskDao().updateTask(updatedTask)
                                    
                                    val nextReminder = reminder.copy(triggerTime = nextTriggerTime)
                                    db.reminderDao().updateReminder(nextReminder)
                                    
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
     * Calculates the next trigger time based on the repeat type.
     */
    private fun calculateNextOccurrence(currentTriggerTime: Long, repeatType: String, repeatDays: String?): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTriggerTime
        }
        val now = System.currentTimeMillis()

        // FIXED: Added logic for Custom Repeat Days
        if (repeatType == "CUSTOM") {
            val daysList = repeatDays?.split(",")?.map { it.trim() } ?: emptyList()
            if (daysList.isEmpty()) return -1L

            var iterations = 0
            // Max 7 days to loop through, +1 for safety
            while (iterations < 8) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val dayName = when (dayOfWeek) {
                    Calendar.MONDAY -> "Mon"
                    Calendar.TUESDAY -> "Tue"
                    Calendar.WEDNESDAY -> "Wed"
                    Calendar.THURSDAY -> "Thu"
                    Calendar.FRIDAY -> "Fri"
                    Calendar.SATURDAY -> "Sat"
                    Calendar.SUNDAY -> "Sun"
                    else -> ""
                }
                
                if (daysList.contains(dayName) && calendar.timeInMillis > now) {
                    return calendar.timeInMillis
                }
                iterations++
            }
            return -1L
        }

        // Standard repeat types
        when (repeatType) {
            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
            else -> return -1L
        }
        
        var nextTime = calendar.timeInMillis
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
