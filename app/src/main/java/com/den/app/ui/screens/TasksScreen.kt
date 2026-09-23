package com.den.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.den.app.AppContainer
import com.den.app.data.model.Task
import com.den.app.ui.components.ChipItem
import com.den.app.ui.components.ConfirmDialog
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.EmptyState
import com.den.app.ui.components.FilterChipRow
import com.den.app.ui.components.SectionHeader
import com.den.app.ui.components.SwipeableTaskRow
import com.den.app.ui.components.TaskContextMenu
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
    data object ArchivedHeader : ListRow
    data class ArchivedItem(val item: TaskListItem) : ListRow
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
    val labelMap by vm.labelMap.collectAsState()
    val archived by vm.archivedRows.collectAsState()

    var searching by remember { mutableStateOf(false) }
    var menuItem by remember { mutableStateOf<TaskListItem?>(null) }
    var confirmDelete by remember { mutableStateOf<TaskListItem?>(null) }

    fun toggleDone(item: TaskListItem) {
        menuItem = null
        val task = item.task
        if (task.completed) vm.uncomplete(task)
        else if (settings.ratingOnComplete) onCompleteTask(task.id) else vm.complete(task, null, null)
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
                    IconButton(onClick = { searching = !searching; if (!searching) vm.setSearch("") }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
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
                    actionText = if (search.isNotBlank()) null else "Add task",
                    onAction = if (search.isNotBlank()) null else onNewTask,
                )
            } else {
                val rows = remember(tasks, archived, filter) {
                    if (filter == TaskFilter.ALL) {
                        val bucketed = tasks
                            .groupBy { bucketOf(it.task) }
                            .toSortedMap(compareBy { it.ordinal })
                            .flatMap { (bucket, items) ->
                                buildList<ListRow> {
                                    add(ListRow.Header(bucket))
                                    addAll(items.map { ListRow.Item(it) })
                                }
                            }
                        if (archived.isEmpty()) bucketed else bucketed + buildList<ListRow> {
                            add(ListRow.ArchivedHeader)
                            addAll(archived.map { ListRow.ArchivedItem(it) })
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
                                ListRow.ArchivedHeader -> "archived-header"
                                is ListRow.ArchivedItem -> "a-${row.item.task.id}"
                            }
                        },
                    ) { row ->
                        when (row) {
                            is ListRow.Header -> SectionHeader(
                                text = bucketLabel(row.bucket),
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp),
                            )
                            ListRow.ArchivedHeader -> SectionHeader(
                                text = "Archived",
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp),
                            )
                            is ListRow.Item -> SwipeableTaskRow(
                                task = row.item.task,
                                labels = row.item.labels,
                                subtasksDone = row.item.done,
                                subtasksTotal = row.item.total,
                                onClick = { onOpenTask(row.item.task.id) },
                                onComplete = {
                                    if (row.item.task.completed) vm.uncomplete(row.item.task)
                                    else if (settings.ratingOnComplete) onCompleteTask(row.item.task.id) else vm.complete(row.item.task, null, null)
                                },
                                onOpenActions = { menuItem = row.item },
                            )
                            is ListRow.ArchivedItem -> TaskRow(
                                task = row.item.task,
                                labels = row.item.labels,
                                subtasksDone = row.item.done,
                                subtasksTotal = row.item.total,
                                onClick = { onOpenTask(row.item.task.id) },
                                onCheck = {},
                                onLongPress = { menuItem = row.item },
                            )
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
            onArchive = {
                vm.archive(item.task)
                menuItem = null
            },
            onRestore = if (item.task.archived) {
                {
                    vm.unarchive(item.task)
                    menuItem = null
                }
            } else null,
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