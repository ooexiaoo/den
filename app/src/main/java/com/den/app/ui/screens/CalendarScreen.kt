package com.den.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.den.app.ui.components.ConfirmDialog
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.EmptyState
import com.den.app.ui.components.MonthCalendar
import com.den.app.ui.components.SwipeableTaskRow
import com.den.app.ui.components.TaskContextMenu
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.TaskListItem
import com.den.app.ui.viewmodel.TasksViewModel
import com.den.app.util.Dates

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    container: AppContainer,
    onOpenTask: (Long) -> Unit,
    onNewTask: () -> Unit,
    onCompleteTask: (Long) -> Unit,
    onFocus: (Long) -> Unit,
) {
    val vm: TasksViewModel = viewModel(factory = DenViewModelFactory(container))
    val allTasks by vm.allTasks.collectAsState()
    val labelMap by vm.labelMap.collectAsState()
    val settings by vm.settings.collectAsState()

    var selectedDayMillis by remember { mutableStateOf(Dates.startOfDay(Dates.now())) }
    var monthOffset by remember { mutableStateOf(0L) }
    var menuItem by remember { mutableStateOf<TaskListItem?>(null) }
    var confirmDelete by remember { mutableStateOf<TaskListItem?>(null) }

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

    fun menuItemFor(task: Task): TaskListItem = TaskListItem(task, 0, 0, labelMap[task.id] ?: emptyList())

    fun toggleDone(item: TaskListItem) {
        menuItem = null
        val task = item.task
        if (task.completed) vm.uncomplete(task)
        else if (settings.ratingOnComplete) onCompleteTask(task.id) else vm.complete(task, null, null)
    }

    Scaffold(
        topBar = {
            DenTopBar(
                title = "Calendar",
                subtitle = Dates.humanDay(selectedDayMillis, hasTime = false),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTask) {
                Icon(Icons.Filled.Add, contentDescription = "New task")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
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
                        SwipeableTaskRow(
                            task = task,
                            labels = labelMap[task.id] ?: emptyList(),
                            subtasksDone = 0,
                            subtasksTotal = 0,
                            onClick = { onOpenTask(task.id) },
                            onComplete = {
                                if (task.completed) vm.uncomplete(task)
                                else if (settings.ratingOnComplete) onCompleteTask(task.id) else vm.complete(task, null, null)
                            },
                            onOpenActions = { menuItem = menuItemFor(task) },
                        )
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
            onFocus = {
                menuItem = null
                onFocus(item.task.id)
            },
            onDuplicate = {
                vm.duplicate(item.task)
                menuItem = null
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