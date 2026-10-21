package xyz.ximanta.daybell.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import xyz.ximanta.daybell.data.local.Reminder
import xyz.ximanta.daybell.data.local.Task
import xyz.ximanta.daybell.receivers.AlarmReceiver

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules an exact alarm for the given task and reminder.
     * Also schedules an advance alert if specified in the reminder.
     */
    fun schedule(task: Task, reminder: Reminder) {
        val triggerTime = reminder.triggerTime

        // Ensure we don't schedule alarms in the past
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        // 1. Schedule the Main Alarm
        val mainIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", task.id)
            putExtra("EXTRA_TASK_TITLE", task.title)
            putExtra("EXTRA_TASK_DESC", task.description)
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_REPEAT_TYPE", reminder.repeatType)
            putExtra("EXTRA_SNOOZE_DURATION", reminder.snoozeDurationMinutes)
            putExtra("EXTRA_IS_ADVANCE_ALERT", false)
        }
        val mainOperation = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExact(triggerTime, mainOperation)

        // 2. Schedule the Advance Alert (if configured)
        if (reminder.advanceAlertMinutes > 0) {
            val advanceTriggerTime = triggerTime - (reminder.advanceAlertMinutes * 60 * 1000L)
            
            // Only schedule if it's in the future
            if (advanceTriggerTime > System.currentTimeMillis()) {
                val advanceIntent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.id)
                    putExtra("EXTRA_TASK_TITLE", task.title)
                    putExtra("EXTRA_TASK_DESC", task.description)
                    putExtra("EXTRA_REMINDER_ID", reminder.id)
                    putExtra("EXTRA_REPEAT_TYPE", reminder.repeatType)
                    putExtra("EXTRA_SNOOZE_DURATION", reminder.snoozeDurationMinutes)
                    putExtra("EXTRA_IS_ADVANCE_ALERT", true)
                }
                // Unique request code for advance alert
                val advanceOperation = PendingIntent.getBroadcast(
                    context,
                    reminder.id.toInt() + 100000,
                    advanceIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                scheduleExact(advanceTriggerTime, advanceOperation)
            }
        }
    }

    private fun scheduleExact(triggerTime: Long, operation: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Permission denied: Fallback to inexact alarm
                Log.w("AlarmScheduler", "Exact alarm permission denied. Falling back to inexact.")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    operation
                )
            } else {
                // Permission granted: Use setExactAndAllowWhileIdle for precise delivery
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    operation
                )
            }
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling alarm, using fallback", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                operation
            )
        }
    }

    /**
     * Cancels all existing alarms (main, advance, and snooze) for the given reminder.
     */
    fun cancel(reminderId: Long) {
        // Cancel Main Alarm
        val mainIntent = Intent(context, AlarmReceiver::class.java)
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(mainPendingIntent)

        // Cancel Advance Alert
        val advancePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 100000,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(advancePendingIntent)

        // Cancel Snoozed Alarm (if any)
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 500000,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(snoozePendingIntent)
    }
}
