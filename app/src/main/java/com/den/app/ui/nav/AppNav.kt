package com.den.app.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.den.app.ui.screens.RoadmapSections
import com.den.app.ui.screens.ReviewScreen
import com.den.app.ui.screens.SearchScreen
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
    const val SEARCH = "search"
    const val MORE = "more"
    const val REVIEW = "review"
    const val ROADMAP = "roadmap/{slug}"

    fun taskDetail(id: Long) = "task/$id"
    fun taskEdit(id: Long?) = if (id == null) "taskEdit" else "taskEdit?taskId=$id"
    fun complete(id: Long) = "complete/$id"
    fun focus(id: Long) = "focus/$id"
    fun noteEdit(id: Long?) = if (id == null) "noteEdit" else "noteEdit?noteId=$id"
    fun labelDetail(id: Long) = "label/$id"
    fun roadmap(slug: String) = "roadmap/$slug"
}

private data class NavItem(val route: String, val icon: ImageVector, val label: String)

private data class CreateOption(
    val icon: ImageVector,
    val label: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

/** Switch from a bottom bar to a side rail once the window gets roomy. */
private const val TABLET_BREAKPOINT_DP = 840

@OptIn(ExperimentalMaterial3Api::class)
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

    val items = listOf(
        NavItem(Routes.HOME, Icons.Filled.Home, "Home"),
        NavItem(Routes.TASKS, Icons.Filled.Checklist, "Tasks"),
        NavItem(Routes.NOTES, Icons.Filled.Description, "Notes"),
        NavItem(Routes.CALENDAR, Icons.Filled.CalendarMonth, "Calendar"),
        NavItem(Routes.MORE, Icons.Filled.MoreHoriz, "More"),
    )
    val onTopLevel = items.any { it.route == currentRoute }
    val showGlobalCreate = onTopLevel && (currentRoute == Routes.HOME || currentRoute == Routes.MORE)

    var handledInitial by rememberSaveable { mutableStateOf(initialTaskId == null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }

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

    val createOptions = listOf(
        CreateOption(Icons.Filled.TaskAlt, "Task", "A single to-do with optional date") {
            showCreate = false
            navController.navigate(Routes.taskEdit(null))
        },
        CreateOption(Icons.Filled.EditNote, "Note", "A free-form note") {
            showCreate = false
            navController.navigate(Routes.noteEdit(null))
        },
        CreateOption(Icons.Filled.Inbox, "Quick Note", "Fast capture into the inbox") {
            showCreate = false
            navController.navigate(Routes.roadmap(RoadmapSections.INBOX))
        },
        CreateOption(Icons.Filled.Folder, "Project", "Group work toward an outcome") {
            showCreate = false
            navController.navigate(Routes.roadmap(RoadmapSections.PROJECTS))
        },
        CreateOption(Icons.Filled.Event, "Event", "Block time on the calendar") {
            showCreate = false
            navController.navigate(Routes.CALENDAR)
        },
        CreateOption(Icons.Filled.Lightbulb, "Idea", "Capture a thought before it slips away") {
            showCreate = false
            navController.navigate(Routes.roadmap(RoadmapSections.INBOX))
        },
        CreateOption(Icons.Filled.Checklist, "Checklist", "A task with subtasks") {
            showCreate = false
            navController.navigate(Routes.taskEdit(null))
        },
    )

    val navigateTab: (NavItem) -> Unit = { item ->
        navController.navigate(item.route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= TABLET_BREAKPOINT_DP.dp

        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            Scaffold(
                floatingActionButton = {
                    if (showGlobalCreate && !useRail) {
                        FloatingActionButton(onClick = { showCreate = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Create")
                        }
                    }
                },
                bottomBar = {
                    if (onTopLevel && !useRail) {
                        NavigationBar {
                            items.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = { navigateTab(item) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                    if (onTopLevel && useRail) {
                        NavigationRail {
                            NavigationRailItem(
                                selected = false,
                                onClick = { showCreate = true },
                                icon = { Icon(Icons.Filled.Add, contentDescription = "Create") },
                                label = { Text("New") },
                            )
                            items.forEach { item ->
                                NavigationRailItem(
                                    selected = currentRoute == item.route,
                                    onClick = { navigateTab(item) },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                )
                            }
                        }
                    }
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        modifier = Modifier.weight(1f),
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                container = container,
                                onOpenTask = { navController.navigate(Routes.taskDetail(it)) },
                                onOpenNote = { navController.navigate(Routes.noteEdit(it)) },
                                onNewTask = { navController.navigate(Routes.taskEdit(null)) },
                                onNewNote = { id -> navController.navigate(Routes.noteEdit(id)) },
                                onCompleteTask = { navController.navigate(Routes.complete(it)) },
                                onOpenSearch = { navController.navigate(Routes.SEARCH) },
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
                        composable(Routes.SEARCH) {
                            SearchScreen(
                                container = container,
                                onBack = { navController.popBackStack() },
                                onOpenTask = { navController.navigate(Routes.taskDetail(it)) },
                                onOpenNote = { navController.navigate(Routes.noteEdit(it)) },
                            )
                        }
                        composable(Routes.MORE) {
                            MoreScreen(
                                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                                onOpenLabels = { navController.navigate(Routes.LABELS) },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                onOpenReview = { navController.navigate(Routes.REVIEW) },
                                onOpenRoadmap = { slug -> navController.navigate(Routes.roadmap(slug)) },
                            )
                        }
                        composable(Routes.REVIEW) {
                            ReviewScreen(
                                container = container,
                                onBack = { navController.popBackStack() },
                                onOpenTask = { navController.navigate(Routes.taskDetail(it)) },
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
    }

    if (showCreate) {
        ModalBottomSheet(onDismissRequest = { showCreate = false }) {
            CreateSheetContent(options = createOptions)
        }
    }
}

@Composable
private fun CreateSheetContent(options: List<CreateOption>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Create",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = option.onClick)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = option.label,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = option.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}