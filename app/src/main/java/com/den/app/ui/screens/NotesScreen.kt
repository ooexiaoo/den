package com.den.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.den.app.AppContainer
import com.den.app.data.model.Note
import com.den.app.ui.components.ChipItem
import com.den.app.ui.components.ConfirmDialog
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.EmptyState
import com.den.app.ui.components.FilterChipRow
import com.den.app.ui.components.NoteCard
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.NoteFilter
import com.den.app.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    container: AppContainer,
    onOpenNote: (Long) -> Unit,
    onNewNote: (Long) -> Unit,
) {
    val vm: NotesViewModel = viewModel(factory = DenViewModelFactory(container))
    val notes by vm.visibleNotes.collectAsState()
    val search by vm.search.collectAsState()
    val filter by vm.filter.collectAsState()
    val scope = rememberCoroutineScope()

    var searching by remember { mutableStateOf(false) }
    var menuNote by remember { mutableStateOf<Note?>(null) }
    var deleting by remember { mutableStateOf<Note?>(null) }

    val subtitle = when (filter) {
        NoteFilter.ALL -> "All notes"
        NoteFilter.PINNED -> "Pinned only"
        NoteFilter.ARCHIVED -> "Archived"
    }

    Scaffold(
        topBar = {
            DenTopBar(
                title = "Notes",
                subtitle = subtitle,
                titleContent = if (searching) {
                    {
                        OutlinedTextField(
                            value = search,
                            onValueChange = vm::setSearch,
                            placeholder = { Text("Search notes") },
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
            FloatingActionButton(onClick = {
                scope.launch { onNewNote(vm.createDraft()) }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "New note")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FilterChipRow(
                items = listOf(
                    ChipItem("All", filter == NoteFilter.ALL) { vm.setFilter(NoteFilter.ALL) },
                    ChipItem("Pinned", filter == NoteFilter.PINNED) { vm.setFilter(NoteFilter.PINNED) },
                    ChipItem("Archived", filter == NoteFilter.ARCHIVED) { vm.setFilter(NoteFilter.ARCHIVED) },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (notes.isEmpty()) {
                EmptyState(
                    title = when (filter) {
                        NoteFilter.PINNED -> "No pinned notes"
                        NoteFilter.ARCHIVED -> "No archived notes"
                        NoteFilter.ALL -> "No notes yet"
                    },
                    subtitle = if (search.isNotBlank()) "No results for \"$search\"" else "Tap + to write a note",
                    actionText = if (filter == NoteFilter.ALL && search.isBlank()) "Create note" else null,
                    onAction = if (filter == NoteFilter.ALL && search.isBlank()) {
                        { scope.launch { onNewNote(vm.createDraft()) } }
                    } else null,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                ) {
                    items(notes, key = { it.note.id }) { item ->
                        Box(modifier = Modifier.padding(vertical = 2.dp)) {
                            NoteCard(
                                note = item.note,
                                labels = item.labels,
                                backlinks = item.backlinkCount,
                                attachments = item.attachmentCount,
                                onClick = { onOpenNote(item.note.id) },
                            )
                            IconButton(
                                onClick = { menuNote = item.note },
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp),
                            ) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(
                                expanded = menuNote == item.note,
                                onDismissRequest = { menuNote = null },
                            ) {
                                if (filter == NoteFilter.ARCHIVED) {
                                    DropdownMenuItem(
                                        text = { Text("Restore") },
                                        onClick = {
                                            menuNote = null
                                            vm.restore(item.note)
                                        },
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text(if (item.note.pinned) "Unpin" else "Pin") },
                                        onClick = {
                                            menuNote = null
                                            vm.togglePinned(item.note)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Archive") },
                                        onClick = {
                                            menuNote = null
                                            vm.archive(item.note)
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        menuNote = null
                                        deleting = item.note
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deleting?.let { note ->
        ConfirmDialog(
            title = "Delete note?",
            message = "This note and its media will be removed permanently.",
            onConfirm = {
                vm.delete(note)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}