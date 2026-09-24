package com.den.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.den.app.AppContainer
import com.den.app.settings.Settings
import com.den.app.ui.components.DenButton
import com.den.app.ui.components.DenCard
import com.den.app.ui.components.DenProgress
import com.den.app.ui.components.EmptyState
import com.den.app.ui.components.NoteCard
import com.den.app.ui.components.SectionHeader
import com.den.app.ui.components.TaskRow
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.HomeViewModel
import com.den.app.util.Dates
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenTask: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
    onNewTask: () -> Unit,
    onNewNote: (Long) -> Unit,
    onCompleteTask: (Long) -> Unit,
    onOpenSearch: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(factory = DenViewModelFactory(container))
    val todayRows by vm.todayRows.collectAsState()
    val upcomingRows by vm.upcomingRows.collectAsState()
    val recentNotes by vm.recentNotes.collectAsState()
    val progress by vm.dayProgress.collectAsState()
    val openCount by vm.openCount.collectAsState()
    val settings by container.settings.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()

    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour < 5 -> "Working late"
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val dateLabel = remember {
        val ld = Dates.toLocalDate(Dates.now())
        "${ld.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
            "${ld.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${ld.dayOfMonth}"
    }

    Scaffold { padding ->
        if (todayRows.isEmpty() && upcomingRows.isEmpty() && recentNotes.isEmpty()) {
            EmptyState(
                title = "Your world is quiet",
                subtitle = "Add a task or a note to start shaping your day.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            ) {
                item {
                    Greeting(
                        greeting = greeting,
                        dateLabel = dateLabel,
                        openCount = openCount,
                        onOpenSearch = onOpenSearch,
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DenButton(
                            text = "New task",
                            onClick = onNewTask,
                            icon = Icons.Filled.Add,
                            modifier = Modifier.weight(1f),
                        )
                        DenButton(
                            text = "New note",
                            onClick = { scope.launch { onNewNote(vm.createNote()) } },
                            icon = Icons.Filled.NoteAdd,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (todayRows.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = "Today",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    item {
                        DenCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            DenProgress(
                                done = progress.first,
                                total = progress.second,
                            )
                        }
                    }
                    items(todayRows, key = { "today-${it.task.id}" }) { item ->
                        TaskRow(
                            task = item.task,
                            labels = emptyList(),
                            subtasksDone = item.done,
                            subtasksTotal = item.total,
                            onClick = { onOpenTask(item.task.id) },
                            onCheck = {
                                if (item.task.completed) vm.uncomplete(item.task)
                                else if (settings.ratingOnComplete) onCompleteTask(item.task.id)
                                else vm.complete(item.task)
                            },
                        )
                    }
                }

                if (upcomingRows.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = "Upcoming",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    items(upcomingRows, key = { "up-${it.task.id}" }) { item ->
                        TaskRow(
                            task = item.task,
                            labels = emptyList(),
                            subtasksDone = item.done,
                            subtasksTotal = item.total,
                            onClick = { onOpenTask(item.task.id) },
                            onCheck = {
                                if (item.task.completed) vm.uncomplete(item.task)
                                else if (settings.ratingOnComplete) onCompleteTask(item.task.id)
                                else vm.complete(item.task)
                            },
                        )
                    }
                }

                if (recentNotes.isNotEmpty()) {
                    item {
                        SectionHeader(
                            text = "Recent notes",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    items(recentNotes, key = { "note-${it.note.id}" }) { item ->
                        NoteCard(
                            note = item.note,
                            labels = item.labels,
                            backlinks = item.backlinkCount,
                            attachments = item.attachmentCount,
                            onClick = { onOpenNote(item.note.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting(
    greeting: String,
    dateLabel: String,
    openCount: Int,
    onOpenSearch: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dateLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$greeting.",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        val message = when {
            openCount == 0 -> "All caught up — enjoy the calm."
            openCount == 1 -> "1 task still open."
            else -> "$openCount tasks still open."
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}