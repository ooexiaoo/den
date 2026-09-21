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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.den.app.ui.components.EmptyState
import com.den.app.ui.components.TaskRow
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.TaskFilter
import com.den.app.ui.viewmodel.TasksViewModel

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

    var searching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searching) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = vm::setSearch,
                            placeholder = { Text("Search tasks") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text("Den")
                    }
                },
                actions = {
                    IconButton(onClick = { searching = !searching; if (!searching) vm.setSearch("") }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTask) {
                Icon(Icons.Filled.Add, contentDescription = "New task")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = filter == TaskFilter.ALL,
                    onClick = { vm.setFilter(TaskFilter.ALL) },
                    label = { Text("Open") },
                )
                FilterChip(
                    selected = filter == TaskFilter.TODAY,
                    onClick = { vm.setFilter(TaskFilter.TODAY) },
                    label = { Text("Today") },
                )
                FilterChip(
                    selected = filter == TaskFilter.UPCOMING,
                    onClick = { vm.setFilter(TaskFilter.UPCOMING) },
                    label = { Text("Upcoming") },
                )
                FilterChip(
                    selected = filter == TaskFilter.DONE,
                    onClick = { vm.setFilter(TaskFilter.DONE) },
                    label = { Text("Done") },
                )
            }

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
                                if (item.task.completed) {
                                    vm.uncomplete(item.task)
                                } else if (settings.ratingOnComplete) {
                                    onCompleteTask(item.task.id)
                                } else {
                                    vm.complete(item.task, null, null)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}