package com.den.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.den.app.AppContainer
import com.den.app.data.model.Task
import com.den.app.ui.components.ChipItem
import com.den.app.ui.components.ConfirmDialog
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.EmptyState
import com.den.app.ui.components.FilterChipRow
import com.den.app.ui.components.MonthCalendar
import com.den.app.ui.components.SectionHeader
import com.den.app.ui.components.TaskRow
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.TaskFilter
import com.den.app.ui.viewmodel.TaskListItem
import com.den.app.ui.viewmodel.TasksViewModel
import com.den.app.util.Dates

private const val DAY_MILLIS = 86_400_000L

private enum class TitleBucket { OVERDUE, TODAY, TOMORROW, LATER, NO_DATE }

private fun bucketOf(task: Task): TitleBucket {
    val due = task.dueAt ?: return TitleBucket.NO_DATE
    val today = Dates.startOfDay(Dates.now())
    return when {
        Dates.startOfDay(due) < today -> TitleBucket.OVERDUE
        Dates.startOfDay(due) == today -> TitleBucket.TODAY
        Dates.startOfDay(due) == today + DAY_MILLIS -> TitleBucket.TOMORROW
        else -> TitleBucket.LATER
    }
}

private fun bucketLabel(bucket: TitleBucket): String = when (bucket) {
    TitleBucket.OVERDUE -> "Overdue"
    TitleBucket.TODAY -> "Today"
    TitleBucket.TOMORROW -> "Tomorrow"
    TitleBucket.LATER -> "Later"
    TitleBucket.NO_DATE -> "No date"
}

private sealed interface ListRow {
    data class Header(val bucket: TitleBucket) : ListRow
    data class Item(val item: TaskListItem) : ListRow
}

@Composable
private fun ContextMenuRow(
    label: String,
    icon: ImageVector,
    onAction: () -> Unit,
    tint: Color = Color.Unspecified,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAction)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
        )
    }
}

@Composable
private fun RescheduleChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskContextMenu(
    item: TaskListItem,
    onDismiss: () -> Unit,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onReschedule: (Long) -> Unit,
    onClearDate: () -> Unit,
    onDelete: () -> Unit,
) {
    val today = Dates.startOfDay(Dates.now())
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(
                text = item.task.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            if (item.task.completed) {
                ContextMenuRow(label = "Reopen", icon = Icons.Filled.CheckCircle, onAction = onToggleDone)
            } else {
                ContextMenuRow(label = "Mark done", icon = Icons.Filled.Check, onAction = onToggleDone)
            }
            ContextMenuRow(label = "Edit", icon = Icons.Filled.Edit, onAction = onEdit)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Text(
                text = "Reschedule",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                RescheduleChip(label = "Today") { onReschedule(today) }
                RescheduleChip(label = "Tomorrow") { onReschedule(today + DAY_MILLIS) }
                RescheduleChip(label = "Next week") { onReschedule(today + 7 * DAY_MILLIS) }
                if (item.task.dueAt != null) RescheduleChip(label = "No date") { onClearDate() }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            ContextMenuRow(
                label = "Delete",
                icon = Icons.Filled.Delete,
                tint = MaterialTheme.colorScheme.error,
                onAction = onDelete,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    container: AppContainer,
    onOpenTask: (Long) -> Unit,
    onNewTask: () -> Unit,
    onCompleteTask: (Long) -> Unit,
) {
    val vm: TasksViewModel = viewModel(factory = DenViewModelFactory(container))
    val tasks by vm.visibleTasks.collectAsState()
    val filter by vm.filter.collectAsState()
    val search by vm.search.collectAsState()
    val settings by vm.settings.collectAsState()
    val allTasks by vm.allTasks.collectAsState()
    val labelMap by vm.labelMap.collectAsState()

    var searching by remember { mutableStateOf(false) }
    var calendarMode by remember { mutableStateOf(false) }
    var selectedDayMillis by remember { mutableStateOf(Dates.startOfDay(Dates.now())) }
    var monthOffset by remember { mutableStateOf(0L) }
    var menuItem by remember { mutableStateOf<TaskListItem?>(null) }
    var confirmDelete by remember { mutableStateOf<TaskListItem?>(null) }

    fun toggleDone(item: TaskListItem) {
        menuItem = null
        val task = item.task
        if (task.completed) vm.uncomplete(task)
        else if (settings.ratingOnComplete) onCompleteTask(task.id) else vm.complete(task, null, null)
    }

    val todayMillis = Dates.startOfDay(Dates.now())
    val selectedMonth = remember(monthOffset) { Dates.shiftMonth(Dates.startOfDay(Dates.now()), monthOffset) }
    val taskDays = remember(allTasks) {
        allTasks.mapNotNull { Dates.startOfDay(it.dueAt ?: return@mapNotNull null) }.toSet()
    }
    val dayTasks = remember(allTasks, selectedDayMillis) {
        allTasks
            .filter { task -> task.dueAt != null && Dates.startOfDay(task.dueAt!!) == selectedDayMillis }
            .sortedWith(compareByDescending<Task> { it.priority }.thenByDescending { it.createdAt })
    }

    Scaffold(
        topBar = {
            DenTopBar(
                title = "Tasks",
                subtitle = when (filter) {
                    TaskFilter.ALL -> "All open"
                    TaskFilter.TODAY -> "Today"
                    TaskFilter.UPCOMING -> "Upcoming"
                    TaskFilter.OVERDUE -> "Overdue"
                    TaskFilter.DONE -> "Completed"
                },
                titleContent = if (searching) {
                    {
                        OutlinedTextField(
                            value = search,
                            onValueChange = vm::setSearch,
                            placeholder = { Text("Search tasks") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else null,
                actions = {
                    IconButton(onClick = { calendarMode = !calendarMode; searching = false; vm.setSearch("") }) {
                        Icon(
                            imageVector = if (calendarMode) Icons.Filled.ViewList else Icons.Filled.Event,
                            contentDescription = if (calendarMode) "Show list" else "Show calendar",
                        )
                    }
                    if (!calendarMode) {
                        IconButton(onClick = { searching = !searching; if (!searching) vm.setSearch("") }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTask) {
                Icon(Icons.Filled.Add, contentDescription = "New task")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (calendarMode) {
                MonthCalendar(
                    month = selectedMonth,
                    selected = Dates.toLocalDate(selectedDayMillis),
                    today = Dates.toLocalDate(todayMillis),
                    taskDays = taskDays,
                    onSelect = { selectedDayMillis = Dates.dayMillis(it) },
                    onShiftMonth = { delta -> monthOffset += delta },
                )
                Text(
                    text = Dates.humanDay(selectedDayMillis, hasTime = false),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (dayTasks.isEmpty()) {
                    EmptyState(
                        title = "Nothing here",
                        subtitle = "Tap + to add a task for this day",
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        items(dayTasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                labels = labelMap[task.id] ?: emptyList(),
                                subtasksDone = 0,
                                subtasksTotal = 0,
                                onClick = { onOpenTask(task.id) },
                                onCheck = {
                                    if (task.completed) vm.uncomplete(task) else if (settings.ratingOnComplete) onCompleteTask(task.id) else vm.complete(task, null, null)
                                },
                                onLongPress = { menuItem = TaskListItem(task, 0, 0, labelMap[task.id] ?: emptyList()) },
                            )
                        }
                    }
                }
            } else {
                FilterChipRow(
                    items = listOf(
                        ChipItem("Open", filter == TaskFilter.ALL) { vm.setFilter(TaskFilter.ALL) },
                        ChipItem("Today", filter == TaskFilter.TODAY) { vm.setFilter(TaskFilter.TODAY) },
                        ChipItem("Upcoming", filter == TaskFilter.UPCOMING) { vm.setFilter(TaskFilter.UPCOMING) },
                        ChipItem("Overdue", filter == TaskFilter.OVERDUE) { vm.setFilter(TaskFilter.OVERDUE) },
                        ChipItem("Done", filter == TaskFilter.DONE) { vm.setFilter(TaskFilter.DONE) },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                if (tasks.isEmpty()) {
                    EmptyState(
                        title = when (filter) {
                            TaskFilter.DONE -> "No completed tasks yet"
                            TaskFilter.TODAY -> "Nothing due today"
                            TaskFilter.UPCOMING -> "Nothing scheduled"
                            TaskFilter.OVERDUE -> "Nothing overdue — all clear"
                            TaskFilter.ALL -> "All clear — add your first task"
                        },
                        subtitle = if (search.isNotBlank()) "No results for \"$search\"" else "Tap + to create a task",
                    )
                } else {
                    val rows = remember(tasks, filter) {
                        if (filter == TaskFilter.ALL) {
                            tasks
                                .groupBy { bucketOf(it.task) }
                                .toSortedMap(compareBy { it.ordinal })
                                .flatMap { (bucket, items) ->
                                    buildList<ListRow> {
                                        add(ListRow.Header(bucket))
                                        addAll(items.map { ListRow.Item(it) })
                                    }
                                }
                        } else {
                            tasks.map { ListRow.Item(it) }
                        }
                    }
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(
                            items = rows,
                            key = { row ->
                                when (row) {
                                    is ListRow.Header -> "h-${row.bucket.name}"
                                    is ListRow.Item -> "t-${row.item.task.id}"
                                }
                            },
                        ) { row ->
                            when (row) {
                                is ListRow.Header -> SectionHeader(
                                    text = bucketLabel(row.bucket),
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp),
                                )
                                is ListRow.Item -> TaskRow(
                                    task = row.item.task,
                                    labels = row.item.labels,
                                    subtasksDone = row.item.done,
                                    subtasksTotal = row.item.total,
                                    onClick = { onOpenTask(row.item.task.id) },
                                    onCheck = {
                                        if (row.item.task.completed) vm.uncomplete(row.item.task)
                                        else if (settings.ratingOnComplete) onCompleteTask(row.item.task.id) else vm.complete(row.item.task, null, null)
                                    },
                                    onLongPress = { menuItem = row.item },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    menuItem?.let { item ->
        TaskContextMenu(
            item = item,
            onDismiss = { menuItem = null },
            onToggleDone = { toggleDone(item) },
            onEdit = {
                menuItem = null
                onOpenTask(item.task.id)
            },
            onReschedule = { due ->
                vm.reschedule(item.task, due)
                menuItem = null
            },
            onClearDate = {
                vm.reschedule(item.task, null)
                menuItem = null
            },
            onDelete = {
                confirmDelete = item
                menuItem = null
            },
        )
    }

    confirmDelete?.let { item ->
        ConfirmDialog(
            title = "Delete task",
            message = "Delete \"${item.task.title}\"? This cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                vm.delete(item.task, onDeleted = {}, onDeleteOwner = container.attachmentRepo::deleteForOwner)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null },
        )
    }
}