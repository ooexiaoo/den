package com.den.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
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
import com.den.app.ui.components.ConfirmDialog
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.EmptyState
import com.den.app.ui.components.NoteCard
import com.den.app.ui.viewmodel.DenViewModelFactory
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
    val pinnedOnly by vm.pinnedOnly.collectAsState()
    val scope = rememberCoroutineScope()

    var searching by remember { mutableStateOf(false) }
    var menuNote by remember { mutableStateOf<Note?>(null) }
    var deleting by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = {
            DenTopBar(
                title = "Notes",
                subtitle = if (pinnedOnly) "Pinned only" else "All notes",
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
                    IconButton(onClick = { vm.setPinnedOnly(!pinnedOnly) }) {
                        Icon(
                            imageVector = if (pinnedOnly) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pinned only",
                            tint = if (pinnedOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
        if (notes.isEmpty()) {
            EmptyState(
                title = if (pinnedOnly) "No pinned notes" else "No notes yet",
                subtitle = if (search.isNotBlank()) "No results for \"$search\"" else "Tap + to write a note",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        NoteCard(
                            note = note,
                            labels = emptyList(),
                            onClick = { onOpenNote(note.id) },
                        )
                        IconButton(
                            onClick = { menuNote = note },
                            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp),
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(
                            expanded = menuNote == note,
                            onDismissRequest = { menuNote = null },
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (note.pinned) "Unpin" else "Pin") },
                                onClick = {
                                    menuNote = null
                                    vm.togglePinned(note)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menuNote = null
                                    deleting = note
                                },
                            )
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