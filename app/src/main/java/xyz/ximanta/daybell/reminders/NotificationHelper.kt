package xyz.ximanta.daybell.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import xyz.ximanta.daybell.DaybellApplication
import xyz.ximanta.daybell.MainActivity
import xyz.ximanta.daybell.R
import xyz.ximanta.daybell.receivers.AlarmReceiver

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Builds and shows the notification for a fired alarm.
     */
    fun showNotification(
        taskId: Long,
        reminderId: Long,
        title: String,
        description: String?,
        repeatType: String,
        snoozeDuration: Int
    ) {
        // Tap on notification opens the app and deep links to the task
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TASK_ID", taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Done
        val doneIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DONE
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_REMINDER_ID", reminderId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 10000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_REMINDER_ID", reminderId)
            putExtra("EXTRA_SNOOZE_DURATION", snoozeDuration)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 20000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, DaybellApplication.CHANNEL_HIGH)
            .setSmallIcon(R.drawable.ic_daybell_logo)
            .setContentTitle(title)
            .setContentText(description ?: context.getString(R.string.app_name))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(description ?: context.getString(R.string.app_name))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, context.getString(R.string.action_done), donePendingIntent)
            .addAction(0, context.getString(R.string.action_snooze), snoozePendingIntent)

        // If this is a repeating task, let the user know
        if (repeatType != "NONE") {
            builder.setSubText(context.getString(R.string.dashboard_upcoming))
        }

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            ) {
                NotificationManagerCompat.from(context).notify(reminderId.toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission
        }
    }

    fun cancelNotification(reminderId: Long) {
        notificationManager.cancel(reminderId.toInt())
    }
}
