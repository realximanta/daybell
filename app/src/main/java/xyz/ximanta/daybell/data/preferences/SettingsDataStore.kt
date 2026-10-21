package xyz.ximanta.daybell.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for the DataStore instance
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "daybell_settings")

data class UserPreferences(
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val use24HourFormat: Boolean = false,
    val defaultReminderHour: Int = 9,
    val defaultReminderMinute: Int = 0,
    val snoozeDuration: Int = 10, // minutes
    val defaultAlertSound: String = "default",
    val weekStartDay: Int = 1, // 1 = Monday, 7 = Sunday
    val autoRolloverOverdue: Boolean = true // Automatically move overdue tasks to today
)

class SettingsDataStore(private val context: Context) {

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val USE_24H_FORMAT = booleanPreferencesKey("use_24h_format")
        private val DEFAULT_REMINDER_HOUR = intPreferencesKey("default_reminder_hour")
        private val DEFAULT_REMINDER_MINUTE = intPreferencesKey("default_reminder_minute")
        private val SNOOZE_DURATION = intPreferencesKey("snooze_duration")
        private val DEFAULT_ALERT_SOUND = stringPreferencesKey("default_alert_sound")
        private val WEEK_START_DAY = intPreferencesKey("week_start_day")
        private val AUTO_ROLLOVER_OVERDUE = booleanPreferencesKey("auto_rollover_overdue")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            themeMode = preferences[THEME_MODE] ?: "SYSTEM",
            use24HourFormat = preferences[USE_24H_FORMAT] ?: false,
            defaultReminderHour = preferences[DEFAULT_REMINDER_HOUR] ?: 9,
            defaultReminderMinute = preferences[DEFAULT_REMINDER_MINUTE] ?: 0,
            snoozeDuration = preferences[SNOOZE_DURATION] ?: 10,
            defaultAlertSound = preferences[DEFAULT_ALERT_SOUND] ?: "default",
            weekStartDay = preferences[WEEK_START_DAY] ?: 1,
            autoRolloverOverdue = preferences[AUTO_ROLLOVER_OVERDUE] ?: true
        )
    }

    suspend fun updateThemeMode(themeMode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode
        }
    }

    suspend fun update24HourFormat(use24Hour: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USE_24H_FORMAT] = use24Hour
        }
    }

    suspend fun updateDefaultReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_REMINDER_HOUR] = hour
            preferences[DEFAULT_REMINDER_MINUTE] = minute
        }
    }

    suspend fun updateSnoozeDuration(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[SNOOZE_DURATION] = minutes
        }
    }

    suspend fun updateDefaultAlertSound(soundUri: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_ALERT_SOUND] = soundUri
        }
    }

    suspend fun updateWeekStartDay(day: Int) {
        context.dataStore.edit { preferences ->
            preferences[WEEK_START_DAY] = day
        }
    }

    suspend fun updateAutoRolloverOverdue(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_ROLLOVER_OVERDUE] = enabled
        }
    }
}
