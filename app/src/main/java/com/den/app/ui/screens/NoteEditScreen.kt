package com.den.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.den.app.data.db.NoteTitleRow
import com.den.app.data.model.AttachPurposes
import com.den.app.data.model.OwnerTypes
import com.den.app.ui.components.AttachedMediaGrid
import com.den.app.ui.components.ColorDot
import com.den.app.ui.components.ConfirmDialog
import com.den.app.ui.components.MediaPickerBar
import com.den.app.ui.components.paletteColors
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.NoteEditViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    container: AppContainer,
    noteId: Long?,
    onDone: () -> Unit,
    onOpenNote: (Long) -> Unit,
) {
    val vm: NoteEditViewModel = viewModel(factory = DenViewModelFactory(container, id = noteId))
    val scope = rememberCoroutineScope()
    val title by vm.title.collectAsState()
    val body by vm.body.collectAsState()
    val pinned by vm.pinned.collectAsState()
    val color by vm.color.collectAsState()
    val mentionTargets by vm.mentionTargets.collectAsState()
    val labels by vm.allLabels.collectAsState()
    val selectedLabels by vm.selectedLabels.collectAsState()
    val backlinks by vm.backlinks.collectAsState()
    val attachments by vm.attachments.collectAsState()

    var showDelete by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val currentId = noteId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentId == null) "New note" else "Note") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.togglePinned() }) {
                        Icon(
                            imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (pinned) "Unpin" else "Pin",
                            tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Color")
                    }
                    if (currentId != null) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { scope.launch { vm.ensureSaved(); onDone() } }) {
                        Icon(Icons.Filled.Check, contentDescription = "Done")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = vm::onTitle,
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            MentionScaffold(
                body = body,
                matches = mentionMatches(mentionTargets, body, currentId),
                onPickMention = vm::insertMention,
            ) {
                OutlinedTextField(
                    value = body,
                    onValueChange = vm::onBody,
                    label = { Text("Note") },
                    minLines = 12,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionTitle("Labels")
            if (labels.isEmpty()) {
                Text("Create labels from the Labels tab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    labels.forEach { l ->
                        FilterChip(
                            selected = l.label.id in selectedLabels,
                            onClick = { vm.toggleLabel(l.label.id) },
                            label = { Text(l.label.name) },
                        )
                    }
                }
            }

            SectionTitle("Media")
            MediaPickerBar(
                onImportUri = { uri ->
                    val ownerId = vm.ensureSaved() ?: return@MediaPickerBar false
                    container.mediaImporter.importFromUri(uri, OwnerTypes.NOTE, ownerId, AttachPurposes.NOTE) != null
                },
                onImportFile = { file, mime, name ->
                    val ownerId = vm.ensureSaved() ?: return@MediaPickerBar false
                    container.mediaImporter.importFromFile(file, mime, name, OwnerTypes.NOTE, ownerId, AttachPurposes.NOTE) != null
                },
            )
            AttachedMediaGrid(
                attachments = attachments,
                fileFor = container.attachmentRepo::fileFor,
                onDelete = { attachment -> scope.launch { container.attachmentRepo.remove(attachment) } },
            )

            if (backlinks.isNotEmpty()) {
                SectionTitle("Linked from")
                backlinks.forEach { note ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onOpenNote(note.id) }.padding(vertical = 4.dp),
                    ) {
                        Text(
                            text = note.title.ifBlank { "Untitled" },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete note?",
            message = "This note and its media will be removed permanently.",
            onConfirm = {
                showDelete = false
                scope.launch {
                    vm.delete()
                    onDone()
                }
            },
            onDismiss = { showDelete = false },
        )
    }

    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Note color") },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    paletteColors.forEachIndexed { index, c ->
                        ColorDot(
                            color = c,
                            selected = color == index,
                            onClick = { vm.setColor(if (color == index) null else index); showColorPicker = false },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

private fun mentionMatches(
    targets: List<NoteTitleRow>,
    body: String,
    selfId: Long?,
): List<NoteTitleRow> {
    val query = mentionQuery(body) ?: return emptyList()
    return targets
        .filter { it.id != selfId }
        .filter { query.isEmpty() || it.title.contains(query, ignoreCase = true) }
        .take(6)
}

private fun mentionQuery(body: String): String? {
    val lastOpen = body.lastIndexOf("[[")
    if (lastOpen < 0) return null
    val tail = body.substring(lastOpen + 2)
    if (tail.contains("]]") || tail.contains("\n")) return null
    return tail
}

@Composable
private fun MentionScaffold(
    body: String,
    matches: List<NoteTitleRow>,
    onPickMention: (Long, String) -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        content()
        if (matches.isNotEmpty()) {
            Surface(
                modifier = Modifier.padding(top = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    matches.forEach { match ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPickMention(match.id, match.title) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = match.title.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text("[link]", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}