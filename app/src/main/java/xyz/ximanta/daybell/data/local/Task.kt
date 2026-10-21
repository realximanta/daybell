package xyz.ximanta.daybell.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val dateTime: Long, // Stored as Epoch milliseconds for precise sorting and scheduling
    val category: String = "General",
    val priority: Int = 2, // 1 = Low, 2 = Medium, 3 = High
    val isCompleted: Boolean = false,
    val isDeleted: Boolean = false, // Soft delete for the "Restore" feature
    val createdAt: Long = System.currentTimeMillis()
)
