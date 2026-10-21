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
import xyz.ximanta.daybell.ui.screens.DashboardScreen
import xyz.ximanta.daybell.ui.screens.SettingsScreen
import xyz.ximanta.daybell.ui.screens.TaskEditorScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object TaskEditor : Screen("task_editor?taskId={taskId}") {
        fun createRoute(taskId: Long? = null) = "task_editor?taskId=${taskId ?: 0L}"
    }
    object Settings : Screen("settings")
    // TODO: Add AllTasks, Completed, Deleted, Categories, About
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

        composable(
            route = Screen.TaskEditor.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId")
            
            LaunchedEffect(taskId) {
                // If taskId is 0, it's a new task. Otherwise, load the existing one.
                taskEditorViewModel.loadTask(if (taskId == 0L) null else taskId)
            }

            TaskEditorScreen(
                viewModel = taskEditorViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
