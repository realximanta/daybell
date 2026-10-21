package xyz.ximanta.daybell.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val triggerTime: Long, // Epoch milliseconds of the next trigger
    val repeatType: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY, CUSTOM
    val repeatDays: String? = null, // CSV of days for CUSTOM e.g., "1,3,5" (Mon, Wed, Fri)
    val advanceAlertMinutes: Int = 0, // 0 = exactly on time
    val snoozeDurationMinutes: Int = 10 // Default snooze time
)
