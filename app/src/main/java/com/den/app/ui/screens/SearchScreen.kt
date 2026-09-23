package com.den.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import com.den.app.AppContainer
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.EmptyState
import com.den.app.ui.components.NoteCard
import com.den.app.ui.components.SectionHeader
import com.den.app.ui.components.TaskRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
) {
    val tasks by container.taskRepo.observeActive().collectAsState(initial = emptyList())
    val notes by container.noteRepo.observeActive().collectAsState(initial = emptyList())
    val taskLabelJoins by container.labelRepo.observeTaskLabelJoins().collectAsState(initial = emptyList())
    val noteLabelJoins by container.labelRepo.observeNoteLabelJoins().collectAsState(initial = emptyList())

    val taskLabelsByTask = remember(taskLabelJoins) { taskLabelJoins.groupBy({ it.taskId }, { it.label }) }
    val noteLabelsByNote = remember(noteLabelJoins) { noteLabelJoins.groupBy({ it.noteId }, { it.label }) }

    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val q = query.trim()
    val matchedTasks = remember(tasks, taskLabelsByTask, q) {
        tasks
            .filter { task -> !task.completed && (q.isEmpty() || task.title.contains(q, ignoreCase = true) || task.notes.contains(q, ignoreCase = true)) }
            .sortedBy { task -> task.dueAt == null }
            .take(40)
    }
    val matchedNotes = remember(notes, noteLabelsByNote, q) {
        notes
            .filter { note -> q.isEmpty() || note.title.contains(q, ignoreCase = true) || note.body.contains(q, ignoreCase = true) }
            .take(40)
    }

    Scaffold(
        topBar = {
            DenTopBar(
                title = "Search",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search tasks and notes") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                } else null,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
            )

            when {
                q.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "Search everything",
                        subtitle = "Find any task or note from one place.",
                    )
                }
                matchedTasks.isEmpty() && matchedNotes.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "No results",
                        subtitle = "Nothing matches \"$q\".",
                    )
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        if (matchedTasks.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    text = "Tasks",
                                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
                                )
                            }
                            items(matchedTasks, key = { "task-${it.id}" }) { task ->
                                TaskRow(
                                    task = task,
                                    labels = taskLabelsByTask[task.id] ?: emptyList(),
                                    subtasksDone = 0,
                                    subtasksTotal = 0,
                                    onClick = { onOpenTask(task.id) },
                                    onCheck = {
                                        if (task.completed) {
                                            scope.launch { container.taskRepo.uncomplete(task) }
                                        } else {
                                            scope.launch { container.taskRepo.toggleCompletion(task, null, null) }
                                        }
                                    },
                                    onLongPress = { onOpenTask(task.id) },
                                )
                            }
                        }
                        if (matchedNotes.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    text = "Notes",
                                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 2.dp),
                                )
                            }
                            items(matchedNotes, key = { "note-${it.id}" }) { note ->
                                NoteCard(
                                    note = note,
                                    labels = noteLabelsByNote[note.id] ?: emptyList(),
                                    onClick = { onOpenNote(note.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}