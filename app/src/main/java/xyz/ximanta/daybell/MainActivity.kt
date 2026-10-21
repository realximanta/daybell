package xyz.ximanta.daybell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import xyz.ximanta.daybell.data.local.AppDatabase
import xyz.ximanta.daybell.data.preferences.SettingsDataStore
import xyz.ximanta.daybell.data.repository.TaskRepository
import xyz.ximanta.daybell.reminders.AlarmScheduler
import xyz.ximanta.daybell.ui.MainViewModel
import xyz.ximanta.daybell.ui.components.DaybellLockup
import xyz.ximanta.daybell.ui.screens.DashboardScreen
import xyz.ximanta.daybell.ui.theme.DaybellTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val alarmScheduler = AlarmScheduler(applicationContext)
        val repository = TaskRepository(database.taskDao(), database.reminderDao(), alarmScheduler)
        val settingsDataStore = SettingsDataStore(applicationContext)
        
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository, settingsDataStore) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val userPreferences by viewModel.userPreferences.collectAsStateWithLifecycle()
            val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle()
            val completedTasks by viewModel.completedTasks.collectAsStateWithLifecycle()

            val isDarkTheme = when (userPreferences.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            DaybellTheme(darkTheme = isDarkTheme) {
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            DaybellLockup(
                                modifier = Modifier.padding(24.dp),
                                showTagline = true
                            )
                            
                            val navItems = listOf("Home", "All Tasks", "Today", "Upcoming", "Completed", "Categories", "Settings", "About")
                            navItems.forEach { item ->
                                NavigationDrawerItem(
                                    label = { Text(item) },
                                    selected = item == "Home",
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        // TODO: Handle navigation to $item
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    }
                ) {
                    DashboardScreen(
                        activeTasks = activeTasks,
                        completedTasks = completedTasks,
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onAddTaskClick = { /* TODO: Navigate to Task Editor */ },
                        onTaskClick = { /* TODO: Open Task Detail */ },
                        onToggleCompletion = { task, isChecked ->
                            viewModel.toggleTaskCompletion(task.id, isChecked)
                        }
                    )
                }
            }
        }
    }
}
