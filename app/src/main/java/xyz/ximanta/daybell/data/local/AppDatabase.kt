package xyz.ximanta.daybell.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Task::class, Reminder::class, Category::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daybell_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // FIXED: Seed default categories on first launch
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                val categoryDao = database.categoryDao()
                                categoryDao.insertCategory(Category(name = "General", colorHex = "#48E0D0"))
                                categoryDao.insertCategory(Category(name = "Work", colorHex = "#A78BFA"))
                                categoryDao.insertCategory(Category(name = "Personal", colorHex = "#0B7A72"))
                                categoryDao.insertCategory(Category(name = "Urgent", colorHex = "#FF5252"))
                            }
                        }
                    }
                })
                // Fallback to destructive migration for early development
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
