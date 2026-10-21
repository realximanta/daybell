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
import java.util.Calendar

class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("SystemEventReceiver", "Received system event: $action")

        // We handle:
        // 1. Phone restart (BOOT_COMPLETED)
        // 2. Time/Timezone change (TIME_SET, TIMEZONE_CHANGED)
        // 3. App updated (MY_PACKAGE_REPLACED)
        // 4. Exact alarm permission granted/revoked (ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
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
        val now = System.currentTimeMillis()

        // Fetch all uncompleted, undeleted tasks
        val tasks = db.taskDao().getUpcomingTasksForReschedule()
        
        for (task in tasks) {
            val reminders = db.reminderDao().getRemindersForTaskSync(task.id)
            for (reminder in reminders) {
                var triggerTime = reminder.triggerTime
                
                // If the trigger time is in the past, we need to fix it.
                if (triggerTime <= now) {
                    if (reminder.repeatType != "NONE") {
                        // Calculate the next future occurrence for repeating tasks
                        triggerTime = calculateNextFutureOccurrence(triggerTime, reminder.repeatType)
                        
                        // Update the database with the new future time
                        val updatedReminder = reminder.copy(triggerTime = triggerTime)
                        db.reminderDao().updateReminder(updatedReminder)
                        
                        // Schedule the new future alarm
                        alarmScheduler.schedule(task, updatedReminder)
                    } else {
                        // For one-time tasks, the alarm was missed while the phone was off.
                        // We don't schedule it in the past.
                        Log.d("SystemEventReceiver", "Missed one-time alarm for task ${task.id}")
                    }
                } else {
                    // Trigger time is in the future, just reschedule it
                    alarmScheduler.schedule(task, reminder)
                }
            }
        }
        Log.d("SystemEventReceiver", "Finished rescheduling ${tasks.size} tasks.")
    }

    private fun calculateNextFutureOccurrence(currentTriggerTime: Long, repeatType: String): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTriggerTime
        }
        val now = System.currentTimeMillis()

        while (calendar.timeInMillis <= now) {
            when (repeatType) {
                "DAILY" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> calendar.add(Calendar.MONTH, 1)
                "YEARLY" -> calendar.add(Calendar.YEAR, 1)
                else -> return currentTriggerTime // Fallback, shouldn't happen
            }
        }
        return calendar.timeInMillis
    }
}
