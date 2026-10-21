package xyz.ximanta.daybell.receivers

import android.app.AlarmManager
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

class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("SystemEventReceiver", "Received system event: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            
            val appContext = context.applicationContext
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    rescheduleAllAlarms(appContext)
                } catch (e: Exception) {
                    Log.e("SystemEventReceiver", "Error rescheduling alarms", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun rescheduleAllAlarms(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val alarmScheduler = AlarmScheduler(context)
        val notificationHelper = NotificationHelper(context) // Added for missed alarms
        val now = System.currentTimeMillis()

        // Fetch all uncompleted, undeleted tasks
        val tasks = db.taskDao().getUpcomingTasksForReschedule()
        
        for (task in tasks) {
            val reminders = db.reminderDao().getRemindersForTaskSync(task.id)
            for (reminder in reminders) {
                var triggerTime = reminder.triggerTime
                
                if (triggerTime <= now) {
                    if (reminder.repeatType != "NONE") {
                        // Calculate the next future occurrence for repeating tasks
                        triggerTime = calculateNextFutureOccurrence(triggerTime, reminder.repeatType, reminder.repeatDays)
                        
                        val updatedReminder = reminder.copy(triggerTime = triggerTime)
                        db.reminderDao().updateReminder(updatedReminder)
                        
                        // Also update the task's dateTime so dashboard doesn't show it as overdue
                        val updatedTask = task.copy(dateTime = triggerTime)
                        db.taskDao().updateTask(updatedTask)
                        
                        alarmScheduler.schedule(updatedTask, updatedReminder)
                    } else {
                        // FIXED: Surface a missed notification for one-time tasks
                        Log.d("SystemEventReceiver", "Missed one-time alarm for task ${task.id}")
                        notificationHelper.showNotification(
                            taskId = task.id,
                            reminderId = reminder.id,
                            title = "Missed: ${task.title}",
                            description = task.description,
                            repeatType = "NONE",
                            snoozeDuration = reminder.snoozeDurationMinutes,
                            isAdvanceAlert = false
                        )
                    }
                } else {
                    // Trigger time is in the future, just reschedule it
                    alarmScheduler.schedule(task, reminder)
                }
            }
        }
        Log.d("SystemEventReceiver", "Finished rescheduling ${tasks.size} tasks.")
    }

    private fun calculateNextFutureOccurrence(currentTriggerTime: Long, repeatType: String, repeatDays: String?): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTriggerTime
        }
        val now = System.currentTimeMillis()

        // Handle CUSTOM repeat days
        if (repeatType == "CUSTOM") {
            val daysList = repeatDays?.split(",")?.map { it.trim() } ?: emptyList()
            if (daysList.isEmpty()) return currentTriggerTime

            var iterations = 0
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
            return currentTriggerTime
        }

        // Handle standard repeat types
        while (calendar.timeInMillis <= now) {
            when (repeatType) {
                "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
                "YEARLY" -> calendar.add(Calendar.YEAR, 1)
                else -> return currentTriggerTime
            }
        }
        return calendar.timeInMillis
    }
}
