package xyz.ximanta.daybell

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

class DaybellApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService<NotificationManager>() ?: return

            // High Priority Channel
            val highChannel = NotificationChannel(
                CHANNEL_HIGH,
                getString(R.string.channel_high_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_high_desc)
                enableVibration(true)
            }

            // Default Channel
            val defaultChannel = NotificationChannel(
                CHANNEL_DEFAULT,
                getString(R.string.channel_default_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.channel_default_desc)
                enableVibration(true)
            }

            // Silent Channel (for advance alerts)
            val silentChannel = NotificationChannel(
                CHANNEL_SILENT,
                getString(R.string.channel_silent_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_silent_desc)
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannels(
                listOf(highChannel, defaultChannel, silentChannel)
            )
        }
    }

    companion object {
        const val CHANNEL_HIGH = "channel_high"
        const val CHANNEL_DEFAULT = "channel_default"
        const val CHANNEL_SILENT = "channel_silent"
    }
}
