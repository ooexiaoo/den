package com.den.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.EmptyState
import com.den.app.ui.components.FilterChipRow
import com.den.app.ui.components.MonthCalendar
import com.den.app.ui.components.TaskRow
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.TaskFilter
import com.den.app.ui.viewmodel.TasksViewModel
import com.den.app.util.Dates

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

    var searching by remember { mutableStateOf(false) }
    var calendarMode by remember { mutableStateOf(false) }
    var selectedDayMillis by remember { mutableStateOf(Dates.startOfDay(Dates.now())) }
    var monthOffset by remember { mutableStateOf(0L) }

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
                                labels = emptyList(),
                                subtasksDone = 0,
                                subtasksTotal = 0,
                                onClick = { onOpenTask(task.id) },
                                onCheck = {
                                    if (task.completed) vm.uncomplete(task) else if (settings.ratingOnComplete) onCompleteTask(task.id) else vm.complete(task, null, null)
                                },
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
                            TaskFilter.ALL -> "All clear — add your first task"
                        },
                        subtitle = if (search.isNotBlank()) "No results for \"$search\"" else "Tap + to create a task",
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        items(tasks, key = { it.task.id }) { item ->
                            TaskRow(
                                task = item.task,
                                labels = emptyList(),
                                subtasksDone = item.done,
                                subtasksTotal = item.total,
                                onClick = { onOpenTask(item.task.id) },
                                onCheck = {
                                    if (item.task.completed) vm.uncomplete(item.task)
                                    else if (settings.ratingOnComplete) onCompleteTask(item.task.id) else vm.complete(item.task, null, null)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}