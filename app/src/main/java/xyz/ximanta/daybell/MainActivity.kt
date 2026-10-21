package xyz.ximanta.daybell

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import xyz.ximanta.daybell.data.local.AppDatabase
import xyz.ximanta.daybell.data.preferences.SettingsDataStore
import xyz.ximanta.daybell.data.repository.BackupManager
import xyz.ximanta.daybell.data.repository.TaskRepository
import xyz.ximanta.daybell.reminders.AlarmScheduler
import xyz.ximanta.daybell.ui.MainViewModel
import xyz.ximanta.daybell.ui.TaskEditorViewModel
import xyz.ximanta.daybell.ui.components.DaybellLockup
import xyz.ximanta.daybell.ui.navigation.DaybellNavGraph
import xyz.ximanta.daybell.ui.navigation.Screen
import xyz.ximanta.daybell.ui.theme.DaybellTheme

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val alarmScheduler by lazy { AlarmScheduler(applicationContext) }
    private val repository by lazy { 
        TaskRepository(database.taskDao(), database.reminderDao(), alarmScheduler) 
    }
    private val settingsDataStore by lazy { SettingsDataStore(applicationContext) }
    private val backupManager by lazy { BackupManager(applicationContext, database) }

    private val mainViewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // FIXED: Injected backupManager to satisfy updated MainViewModel
                return MainViewModel(repository, settingsDataStore, database.categoryDao(), backupManager) as T
            }
        }
    }

    private val taskEditorViewModel: TaskEditorViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskEditorViewModel(repository, database.categoryDao(), settingsDataStore) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val userPreferences by mainViewModel.userPreferences.collectAsStateWithLifecycle()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { /* Optional: Handle UI state */ }
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val isDarkTheme = when (userPreferences.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            DaybellTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            DaybellLockup(
                                modifier = Modifier.padding(24.dp),
                                showTagline = true
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                            
                            val navItems = listOf(
                                "Home" to Screen.Dashboard.route,
                                "All Tasks" to Screen.AllTasks.route,
                                "Today" to Screen.Today.route,
                                "Upcoming" to Screen.Upcoming.route,
                                "Completed" to Screen.Completed.route,
                                "Categories" to Screen.Categories.route,
                                "Settings" to Screen.Settings.route,
                                "About" to Screen.About.route
                            )
                            
                            navItems.forEach { (label, route) ->
                                NavigationDrawerItem(
                                    label = { Text(label) },
                                    selected = currentRoute == route,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (currentRoute != route) {
                                            navController.navigate(route) {
                                                popUpTo(Screen.Dashboard.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    }
                ) {
                    DaybellNavGraph(
                        navController = navController,
                        mainViewModel = mainViewModel,
                        taskEditorViewModel = taskEditorViewModel,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}
