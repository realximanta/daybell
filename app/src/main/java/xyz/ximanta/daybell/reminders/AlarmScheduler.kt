package xyz.ximanta.daybell.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import xyz.ximanta.daybell.MainActivity
import xyz.ximanta.daybell.data.local.Reminder
import xyz.ximanta.daybell.data.local.Task
import xyz.ximanta.daybell.receivers.AlarmReceiver

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules the primary exact alarm for the given task and reminder.
     */
    fun schedule(task: Task, reminder: Reminder) {
        val triggerTime = reminder.triggerTime

        // Ensure we don't schedule alarms in the past
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", task.id)
            putExtra("EXTRA_TASK_TITLE", task.title)
            putExtra("EXTRA_TASK_DESC", task.description)
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_REPEAT_TYPE", reminder.repeatType)
            putExtra("EXTRA_SNOOZE_DURATION", reminder.snoozeDurationMinutes)
            putExtra("EXTRA_IS_SNOOZE", false)
        }

        // Use the actual reminder ID as the request code
        val requestCode = reminder.id.toInt()

        val operation = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(triggerTime, operation, task.id, requestCode)
    }

    /**
     * Schedules a temporary, one-off snooze alarm.
     * Uses a distinct request code so it doesn't overwrite the primary alarm.
     */
    fun scheduleSnooze(task: Task, reminder: Reminder, snoozeTriggerTime: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", task.id)
            putExtra("EXTRA_TASK_TITLE", task.title)
            putExtra("EXTRA_TASK_DESC", task.description)
            putExtra("EXTRA_REMINDER_ID", reminder.id) // Still pass original ID
            putExtra("EXTRA_REPEAT_TYPE", "NONE") // Snoozes do not repeat
            putExtra("EXTRA_SNOOZE_DURATION", reminder.snoozeDurationMinutes)
            putExtra("EXTRA_IS_SNOOZE", true)
        }

        // Use a distinct synthetic request code for the snooze instance to avoid collisions
        val requestCode = reminder.id.toInt() + 100000 

        val operation = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(snoozeTriggerTime, operation, task.id, requestCode)
    }

    /**
     * Helper to set the alarm with exact timing if permitted, otherwise fallback.
     */
    private fun setAlarm(triggerTime: Long, operation: PendingIntent, taskId: Long, requestCode: Int) {
        // PendingIntent for when the user taps the alarm icon in the status bar
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TASK_ID", taskId)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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
                // (Audit recommendation: Avoid setAlarmClock for general tasks)
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    operation
                )
            }
        } catch (e: SecurityException) {
            // Catch edge case where permission is revoked mid-flight
            Log.e("AlarmScheduler", "SecurityException scheduling alarm, using fallback", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                operation
            )
        }
    }

    /**
     * Cancels an existing primary alarm using the reminder's unique ID.
     */
    fun cancel(reminderId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
