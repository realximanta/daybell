package xyz.ximanta.daybell.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.ximanta.daybell.data.local.AppDatabase
import xyz.ximanta.daybell.data.local.Reminder
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

        // Use goAsync() because we are doing database operations in a background coroutine
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_DONE -> {
                        // Mark task complete in DB
                        db.taskDao().setTaskCompletion(taskId, true)
                        // Cancel notification
                        notificationHelper.cancelNotification(reminderId)
                    }

                    ACTION_SNOOZE -> {
                        val snoozeDuration = intent.getIntExtra("EXTRA_SNOOZE_DURATION", 10)
                        notificationHelper.cancelNotification(reminderId)
                        
                        // Fetch the latest reminder to reschedule it
                        val reminders = db.reminderDao().getRemindersForTaskSync(taskId)
                        val reminder = reminders.find { it.id == reminderId }
                        val task = db.taskDao().getTaskByIdSync(taskId)

                        if (reminder != null && task != null) {
                            // Calculate new trigger time
                            val newTriggerTime = System.currentTimeMillis() + (snoozeDuration * 60 * 1000L)
                            val snoozedReminder = reminder.copy(
                                id = reminder.id, // Keep same ID to override/cancel previous
                                triggerTime = newTriggerTime
                            )
                            // Update DB and schedule
                            db.reminderDao().updateReminder(snoozedReminder)
                            alarmScheduler.schedule(task, snoozedReminder)
                        }
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
                        if (repeatType != "NONE") {
                            val reminders = db.reminderDao().getRemindersForTaskSync(taskId)
                            val reminder = reminders.find { it.id == reminderId }
                            val task = db.taskDao().getTaskByIdSync(taskId)

                            if (reminder != null && task != null) {
                                val nextTriggerTime = calculateNextOccurrence(reminder.triggerTime, repeatType)
                                if (nextTriggerTime > 0) {
                                    val nextReminder = reminder.copy(triggerTime = nextTriggerTime)
                                    db.reminderDao().updateReminder(nextReminder)
                                    alarmScheduler.schedule(task, nextReminder)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error processing alarm action", e)
            } finally {
                // Must call finish() to let the system know we are done
                pendingResult.finish()
            }
        }
    }

    /**
     * Calculates the next trigger time based on the repeat type.
     */
    private fun calculateNextOccurrence(currentTriggerTime: Long, repeatType: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTriggerTime
        }

        when (repeatType) {
            "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
            "YEARLY" -> calendar.add(Calendar.YEAR, 1)
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
