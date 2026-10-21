package xyz.ximanta.daybell.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // Filters out completed and deleted tasks for the dashboard
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isCompleted = 0 ORDER BY dateTime ASC")
    fun getAllActiveTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 1 ORDER BY dateTime DESC")
    fun getDeletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND isDeleted = 0 ORDER BY dateTime DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: Long): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskByIdSync(taskId: Long): Task?

    @Query("SELECT * FROM tasks WHERE dateTime >= :startOfDay AND dateTime <= :endOfDay AND isDeleted = 0 AND isCompleted = 0 ORDER BY dateTime ASC")
    fun getTasksForDay(startOfDay: Long, endOfDay: Long): Flow<List<Task>>

    // --- Synchronous queries for Backup/Restore ---
    
    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<Task>

    // --- Mutations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // Soft delete (Move to Recycle Bin)
    @Query("UPDATE tasks SET isDeleted = 1 WHERE id = :taskId")
    suspend fun softDeleteTask(taskId: Long)

    // Restore from Recycle Bin
    @Query("UPDATE tasks SET isDeleted = 0 WHERE id = :taskId")
    suspend fun restoreTask(taskId: Long)

    // Toggle completion state
    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun setTaskCompletion(taskId: Long, isCompleted: Boolean)

    // Get all uncompleted tasks for boot rescheduling
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND isDeleted = 0")
    suspend fun getUpcomingTasksForReschedule(): List<Task>
}
