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
     * Schedules an exact alarm for the given task and reminder.
     * Uses setAlarmClock for maximum reliability (bypasses Doze mode).
     * Falls back to inexact alarms if the user has denied SCHEDULE_EXACT_ALARM on Android 12+.
     */
    fun schedule(task: Task, reminder: Reminder) {
        val triggerTime = reminder.triggerTime

        // Ensure we don't schedule alarms in the past
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        // 1. PendingIntent for the BroadcastReceiver that will show the notification
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", task.id)
            putExtra("EXTRA_TASK_TITLE", task.title)
            putExtra("EXTRA_TASK_DESC", task.description)
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_REPEAT_TYPE", reminder.repeatType)
            putExtra("EXTRA_SNOOZE_DURATION", reminder.snoozeDurationMinutes)
        }

        // Unique request code based on reminder ID
        val requestCode = reminder.id.toInt()

        val operation = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. PendingIntent for when the user taps the alarm icon in the status bar
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TASK_ID", task.id)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Permission denied: Fallback to inexact alarm (may be delayed by Doze mode)
                Log.w("AlarmScheduler", "Exact alarm permission denied. Falling back to inexact.")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    operation
                )
            } else {
                // Permission granted: Use AlarmClock for highest priority delivery
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, operation)
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
     * Cancels an existing alarm using the reminder's unique ID.
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
