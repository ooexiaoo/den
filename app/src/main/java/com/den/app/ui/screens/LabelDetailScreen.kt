package com.den.app.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.den.app.AppContainer
import com.den.app.ui.components.EmptyState
import com.den.app.ui.components.NoteCard
import com.den.app.ui.components.TaskRow
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.LabelDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelDetailScreen(
    container: AppContainer,
    labelId: Long,
    onBack: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
) {
    val vm: LabelDetailViewModel = viewModel(factory = DenViewModelFactory(container, labelId = labelId))
    val label by vm.label.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val notes by vm.notes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(label?.name ?: "")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        ) {
            if (tasks.isEmpty() && notes.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nothing here yet",
                        subtitle = "Tag tasks and notes with \"${label?.name.orEmpty()}\"",
                    )
                }
            }
            if (tasks.isNotEmpty()) {
                item {
                    SectionLabel(text = "Tasks")
                }
                items(tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        labels = emptyList(),
                        subtasksDone = 0,
                        subtasksTotal = 0,
                        onClick = { onOpenTask(task.id) },
                        onCheck = {},
                    )
                }
            }
            if (notes.isNotEmpty()) {
                item {
                    SectionLabel(text = "Notes")
                }
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        labels = emptyList(),
                        onClick = { onOpenNote(note.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        fontSize = 13.sp,
        textAlign = TextAlign.Start,
    )
}