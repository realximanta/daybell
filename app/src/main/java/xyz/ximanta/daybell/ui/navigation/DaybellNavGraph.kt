package xyz.ximanta.daybell.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import xyz.ximanta.daybell.ui.MainViewModel
import xyz.ximanta.daybell.ui.TaskEditorViewModel
import xyz.ximanta.daybell.ui.screens.AboutScreen
import xyz.ximanta.daybell.ui.screens.AllTasksScreen
import xyz.ximanta.daybell.ui.screens.CategoriesScreen
import xyz.ximanta.daybell.ui.screens.CompletedScreen
import xyz.ximanta.daybell.ui.screens.DashboardScreen
import xyz.ximanta.daybell.ui.screens.RecycleBinScreen
import xyz.ximanta.daybell.ui.screens.SettingsScreen
import xyz.ximanta.daybell.ui.screens.TaskEditorScreen
import xyz.ximanta.daybell.ui.screens.TodayScreen
import xyz.ximanta.daybell.ui.screens.UpcomingScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AllTasks : Screen("all_tasks")
    object Today : Screen("today")
    object Upcoming : Screen("upcoming")
    object Completed : Screen("completed")
    object RecycleBin : Screen("recycle_bin") // FIXED: Added Recycle Bin route
    object Categories : Screen("categories")
    object Settings : Screen("settings")
    object About : Screen("about")
    object TaskEditor : Screen("task_editor?taskId={taskId}") {
        fun createRoute(taskId: Long? = null) = "task_editor?taskId=${taskId ?: 0L}"
    }
}

@Composable
fun DaybellNavGraph(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    taskEditorViewModel: TaskEditorViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            val activeTasks by mainViewModel.activeTasks.collectAsStateWithLifecycle()
            val completedTasks by mainViewModel.completedTasks.collectAsStateWithLifecycle()

            DashboardScreen(
                activeTasks = activeTasks,
                completedTasks = completedTasks,
                onMenuClick = onMenuClick,
                onAddTaskClick = { navController.navigate(Screen.TaskEditor.createRoute(null)) },
                onTaskClick = { task -> navController.navigate(Screen.TaskEditor.createRoute(task.id)) },
                onToggleCompletion = { task, isChecked ->
                    mainViewModel.toggleTaskCompletion(task.id, isChecked)
                }
            )
        }

        composable(Screen.AllTasks.route) {
            val allTasks by mainViewModel.activeTasks.collectAsStateWithLifecycle()
            AllTasksScreen(
                allActiveTasks = allTasks,
                onNavigateBack = { navController.popBackStack() },
                onTaskClick = { task -> navController.navigate(Screen.TaskEditor.createRoute(task.id)) },
                onToggleCompletion = { task, isChecked -> mainViewModel.toggleTaskCompletion(task.id, isChecked) }
            )
        }

        composable(Screen.Today.route) {
            val todayTasks by mainViewModel.todayTasks.collectAsStateWithLifecycle()
            TodayScreen(
                tasks = todayTasks,
                onNavigateBack = { navController.popBackStack() },
                onTaskClick = { task -> navController.navigate(Screen.TaskEditor.createRoute(task.id)) },
                onToggleCompletion = { task, isChecked -> mainViewModel.toggleTaskCompletion(task.id, isChecked) }
            )
        }

        composable(Screen.Upcoming.route) {
            val upcomingTasks by mainViewModel.upcomingTasks.collectAsStateWithLifecycle()
            UpcomingScreen(
                upcomingTasks = upcomingTasks,
                onNavigateBack = { navController.popBackStack() },
                onTaskClick = { task -> navController.navigate(Screen.TaskEditor.createRoute(task.id)) },
                onToggleCompletion = { task, isChecked -> mainViewModel.toggleTaskCompletion(task.id, isChecked) }
            )
        }

        composable(Screen.Completed.route) {
            val completedTasks by mainViewModel.completedTasks.collectAsStateWithLifecycle()
            CompletedScreen(
                completedTasks = completedTasks,
                onNavigateBack = { navController.popBackStack() },
                onTaskClick = { task -> navController.navigate(Screen.TaskEditor.createRoute(task.id)) },
                onUncompleteTask = { task -> mainViewModel.toggleTaskCompletion(task.id, false) },
                onDeleteTask = { task -> mainViewModel.deleteTask(task.id) }
            )
        }

        // FIXED: Wired up the Recycle Bin Screen
        composable(Screen.RecycleBin.route) {
            val deletedTasks by mainViewModel.deletedTasks.collectAsStateWithLifecycle()
            RecycleBinScreen(
                deletedTasks = deletedTasks,
                onNavigateBack = { navController.popBackStack() },
                onRestoreTask = { task -> mainViewModel.restoreTask(task.id) },
                onPermanentlyDeleteTask = { task -> mainViewModel.permanentlyDeleteTask(task) }
            )
        }

        composable(Screen.Categories.route) {
            val categories by mainViewModel.allCategories.collectAsStateWithLifecycle()
            CategoriesScreen(
                categories = categories,
                onNavigateBack = { navController.popBackStack() },
                onAddCategory = { name, color -> mainViewModel.addCategory(name, color) },
                onDeleteCategory = { category -> mainViewModel.deleteCategory(category) }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TaskEditor.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId")
            
            LaunchedEffect(taskId) {
                taskEditorViewModel.loadTask(if (taskId == 0L) null else taskId)
            }

            TaskEditorScreen(
                viewModel = taskEditorViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
