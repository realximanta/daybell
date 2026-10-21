package xyz.ximanta.daybell.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import xyz.ximanta.daybell.data.local.AppDatabase
import xyz.ximanta.daybell.data.local.Category
import xyz.ximanta.daybell.data.local.Reminder
import xyz.ximanta.daybell.data.local.Task

class BackupManager(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun exportData(uri: Uri) = withContext(Dispatchers.IO) {
        val rootObject = JSONObject()

        // 1. Export Categories
        val categories = database.categoryDao().getAllCategoriesListSync() // Need to add this to DAO
        val categoriesArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject().apply {
                put("id", cat.id)
                put("name", cat.name)
                put("colorHex", cat.colorHex)
            }
            categoriesArray.put(obj)
        }
        rootObject.put("categories", categoriesArray)

        // 2. Export Tasks
        val tasks = database.taskDao().getAllTasksSync() // Need to add this to DAO
        val tasksArray = JSONArray()
        for (task in tasks) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("dateTime", task.dateTime)
                put("category", task.category)
                put("priority", task.priority)
                put("isCompleted", task.isCompleted)
                put("isDeleted", task.isDeleted)
                put("createdAt", task.createdAt)
            }
            tasksArray.put(obj)
        }
        rootObject.put("tasks", tasksArray)

        // 3. Export Reminders
        val reminders = database.reminderDao().getAllReminders() // Already exists
        val remindersArray = JSONArray()
        for (reminder in reminders) {
            val obj = JSONObject().apply {
                put("id", reminder.id)
                put("taskId", reminder.taskId)
                put("triggerTime", reminder.triggerTime)
                put("repeatType", reminder.repeatType)
                put("repeatDays", reminder.repeatDays)
                put("advanceAlertMinutes", reminder.advanceAlertMinutes)
                put("snoozeDurationMinutes", reminder.snoozeDurationMinutes)
            }
            remindersArray.put(obj)
        }
        rootObject.put("reminders", remindersArray)

        // Write to the user-selected file URI
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(rootObject.toString(2).toByteArray())
        }
    }

    suspend fun importData(uri: Uri) = withContext(Dispatchers.IO) {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        } ?: return@withContext

        val rootObject = JSONObject(jsonString)

        // 1. Import Categories
        if (rootObject.has("categories")) {
            val categoriesArray = rootObject.getJSONArray("categories")
            for (i in 0 until categoriesArray.length()) {
                val obj = categoriesArray.getJSONObject(i)
                val category = Category(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    colorHex = obj.getString("colorHex")
                )
                database.categoryDao().insertCategory(category)
            }
        }

        // 2. Import Tasks
        if (rootObject.has("tasks")) {
            val tasksArray = rootObject.getJSONArray("tasks")
            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                val task = Task(
                    id = obj.getLong("id"),
                    title = obj.getString("title"),
                    description = if (obj.isNull("description")) null else obj.getString("description"),
                    dateTime = obj.getLong("dateTime"),
                    category = obj.getString("category"),
                    priority = obj.getInt("priority"),
                    isCompleted = obj.getBoolean("isCompleted"),
                    isDeleted = obj.getBoolean("isDeleted"),
                    createdAt = obj.getLong("createdAt")
                )
                database.taskDao().insertTask(task)
            }
        }

        // 3. Import Reminders
        if (rootObject.has("reminders")) {
            val remindersArray = rootObject.getJSONArray("reminders")
            for (i in 0 until remindersArray.length()) {
                val obj = remindersArray.getJSONObject(i)
                val reminder = Reminder(
                    id = obj.getLong("id"),
                    taskId = obj.getLong("taskId"),
                    triggerTime = obj.getLong("triggerTime"),
                    repeatType = obj.getString("repeatType"),
                    repeatDays = if (obj.isNull("repeatDays")) null else obj.getString("repeatDays"),
                    advanceAlertMinutes = obj.getInt("advanceAlertMinutes"),
                    snoozeDurationMinutes = obj.getInt("snoozeDurationMinutes")
                )
                database.reminderDao().insertReminder(reminder)
            }
        }
        
        // TODO: After import, we need to reschedule alarms. 
        // This will be handled by a ViewModel update in the next step.
    }
}
