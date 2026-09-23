package com.den.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.den.app.AppContainer
import com.den.app.NavRequest
import com.den.app.ui.components.LocalSnackbarHostState
import com.den.app.ui.screens.CalendarScreen
import com.den.app.ui.screens.CompletionScreen
import com.den.app.ui.screens.FocusScreen
import com.den.app.ui.screens.HomeScreen
import com.den.app.ui.screens.LabelDetailScreen
import com.den.app.ui.screens.LabelsScreen
import com.den.app.ui.screens.MoreScreen
import com.den.app.ui.screens.NoteEditScreen
import com.den.app.ui.screens.NotesScreen
import com.den.app.ui.screens.RoadmapScreen
import com.den.app.ui.screens.SettingsScreen
import com.den.app.ui.screens.TaskDetailScreen
import com.den.app.ui.screens.TaskEditScreen
import com.den.app.ui.screens.TasksScreen

private object Routes {
    const val HOME = "home"
    const val TASKS = "tasks"
    const val TASK_DETAIL = "task/{taskId}"
    const val TASK_EDIT = "taskEdit?taskId={taskId}"
    const val COMPLETE = "complete/{taskId}"
    const val FOCUS = "focus/{taskId}"
    const val NOTES = "notes"
    const val CALENDAR = "calendar"
    const val NOTE_EDIT = "noteEdit?noteId={noteId}"
    const val LABELS = "labels"
    const val LABEL_DETAIL = "label/{labelId}"
    const val SETTINGS = "settings"
    const val MORE = "more"
    const val ROADMAP = "roadmap/{slug}"

    fun taskDetail(id: Long) = "task/$id"
    fun taskEdit(id: Long?) = if (id == null) "taskEdit" else "taskEdit?taskId=$id"
    fun complete(id: Long) = "complete/$id"
    fun focus(id: Long) = "focus/$id"
    fun noteEdit(id: Long?) = if (id == null) "noteEdit" else "noteEdit?noteId=$id"
    fun labelDetail(id: Long) = "label/$id"
}

@Composable
fun AppNav(
    container: AppContainer,
    initialTaskId: Long?,
    onInitialTaskHandled: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomItems = listOf(
        BottomItem(Routes.HOME, Icons.Filled.Home, "Home"),
        BottomItem(Routes.TASKS, Icons.Filled.Checklist, "Tasks"),
        BottomItem(Routes.NOTES, Icons.Filled.Description, "Notes"),
        BottomItem(Routes.CALENDAR, Icons.Filled.CalendarMonth, "Calendar"),
        BottomItem(Routes.MORE, Icons.Filled.MoreHoriz, "More"),
    )
    val showBottomBar = bottomItems.any { it.route == currentRoute }

    var handledInitial by rememberSaveable { mutableStateOf(initialTaskId == null) }

    LaunchedEffect(container) {
        container.navRequests.collect { request ->
            when (request) {
                is NavRequest.OpenTask -> navController.navigate(Routes.taskDetail(request.id)) { launchSingleTop = true }
                is NavRequest.OpenNote -> navController.navigate(Routes.noteEdit(request.id)) { launchSingleTop = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!handledInitial && initialTaskId != null) {
            navController.navigate(Routes.taskDetail(initialTaskId)) { launchSingleTop = true }
            onInitialTaskHandled()
            handledInitial = true
        }
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(padding),
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        container = container,
                        onOpenTask = { navController.navigate(Routes.taskDetail(it)) },
                        onOpenNote = { navController.navigate(Routes.noteEdit(it)) },
                        onNewTask = { navController.navigate(Routes.taskEdit(null)) },
                        onNewNote = { id -> navController.navigate(Routes.noteEdit(id)) },
                        onCompleteTask = { navController.navigate(Routes.complete(it)) },
                    )
                }
                composable(Routes.TASKS) {
                    TasksScreen(
                        container = container,
                        onOpenTask = { navController.navigate(Routes.taskDetail(it)) },
                        onNewTask = { navController.navigate(Routes.taskEdit(null)) },
                        onCompleteTask = { navController.navigate(Routes.complete(it)) },
                        onFocus = { navController.navigate(Routes.focus(it)) },
                    )
                }
                composable(
                    route = Routes.TASK_EDIT,
                    arguments = listOf(navArgument("taskId") { type = NavType.LongType; defaultValue = -1L }),
                ) { entry ->
                    val id = entry.arguments?.getLong("taskId")?.takeIf { it != -1L }
                    TaskEditScreen(
                        container = container,
                        taskId = id,
                        onSaved = {
                            navController.popBackStack()
                            if (id == null) navController.navigate(Routes.taskDetail(it))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Routes.TASK_DETAIL,
                    arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
                ) { entry ->
                    val id = requireNotNull(entry.arguments?.getLong("taskId"))
                    TaskDetailScreen(
                        container = container,
                        taskId = id,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate(Routes.taskEdit(id)) },
                        onComplete = { navController.navigate(Routes.complete(it)) },
                        onFocus = { navController.navigate(Routes.focus(id)) },
                    )
                }
                composable(
                    route = Routes.FOCUS,
                    arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
                ) { entry ->
                    val id = requireNotNull(entry.arguments?.getLong("taskId"))
                    FocusScreen(
                        container = container,
                        taskId = id,
                        onBack = { navController.popBackStack() },
                        onComplete = { navController.navigate(Routes.complete(it)) },
                    )
                }
                composable(
                    route = Routes.COMPLETE,
                    arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
                ) { entry ->
                    val id = requireNotNull(entry.arguments?.getLong("taskId"))
                    CompletionScreen(
                        container = container,
                        taskId = id,
                        onDone = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.NOTES) {
                    NotesScreen(
                        container = container,
                        onOpenNote = { navController.navigate(Routes.noteEdit(it)) },
                        onNewNote = { navController.navigate(Routes.noteEdit(it)) },
                    )
                }
                composable(Routes.CALENDAR) {
                    CalendarScreen(
                        container = container,
                        onOpenTask = { navController.navigate(Routes.taskDetail(it)) },
                        onNewTask = { navController.navigate(Routes.taskEdit(null)) },
                        onCompleteTask = { navController.navigate(Routes.complete(it)) },
                        onFocus = { navController.navigate(Routes.focus(it)) },
                    )
                }
                composable(
                    route = Routes.NOTE_EDIT,
                    arguments = listOf(navArgument("noteId") { type = NavType.LongType; defaultValue = -1L }),
                ) { entry ->
                    val id = entry.arguments?.getLong("noteId")?.takeIf { it != -1L }
                    NoteEditScreen(
                        container = container,
                        noteId = id,
                        onDone = { navController.popBackStack() },
                        onOpenNote = { navController.navigate(Routes.noteEdit(it)) },
                    )
                }
                composable(Routes.LABELS) {
                    LabelsScreen(
                        container = container,
                        onOpenLabel = { navController.navigate(Routes.labelDetail(it)) },
                    )
                }
                composable(
                    route = Routes.LABEL_DETAIL,
                    arguments = listOf(navArgument("labelId") { type = NavType.LongType }),
                ) { entry ->
                    val id = requireNotNull(entry.arguments?.getLong("labelId"))
                    LabelDetailScreen(
                        container = container,
                        labelId = id,
                        onBack = { navController.popBackStack() },
                        onOpenTask = { navController.navigate(Routes.taskDetail(it)) },
                        onOpenNote = { navController.navigate(Routes.noteEdit(it)) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.MORE) {
                    MoreScreen(
                        onOpenLabels = { navController.navigate(Routes.LABELS) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenRoadmap = { slug -> navController.navigate(Routes.ROADMAP.replace("{slug}", slug)) },
                    )
                }
                composable(
                    route = Routes.ROADMAP,
                    arguments = listOf(navArgument("slug") { type = NavType.StringType }),
                ) { entry ->
                    val slug = requireNotNull(entry.arguments?.getString("slug"))
                    RoadmapScreen(
                        slug = slug,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

private data class BottomItem(val route: String, val icon: ImageVector, val label: String)