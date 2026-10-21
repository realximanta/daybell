package xyz.ximanta.daybell.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    fun getRemindersForTask(taskId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    suspend fun getRemindersForTaskSync(taskId: Long): List<Reminder>

    @Query("SELECT * FROM reminders")
    suspend fun getAllReminders(): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun deleteRemindersForTask(taskId: Long)
}
