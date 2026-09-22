package com.den.app.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.den.app.AppContainer
import com.den.app.data.model.AttachPurposes
import com.den.app.data.model.Attachment
import com.den.app.data.model.OwnerTypes
import com.den.app.data.model.Task
import com.den.app.ui.components.AttachedMediaGrid
import com.den.app.ui.components.ConfirmDialog
import com.den.app.ui.components.DueChip
import com.den.app.ui.components.LabelChipsRow
import com.den.app.ui.components.MediaPickerBar
import com.den.app.ui.components.SectionHeader
import com.den.app.ui.components.SubtaskProgress
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.TaskDetailViewModel
import com.den.app.util.Dates
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    container: AppContainer,
    taskId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onComplete: (Long) -> Unit,
) {
    val vm: TaskDetailViewModel = viewModel(factory = DenViewModelFactory(container, id = taskId))
    val data by vm.withSubtasks.collectAsState()
    val labels by vm.labels.collectAsState()
    val settings by container.settings.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val allAttachments by container.attachmentRepo.observeForOwner(OwnerTypes.TASK, taskId)
        .collectAsState(initial = emptyList())
    val contentAttachments = allAttachments.filter { it.purpose == AttachPurposes.TASK_CONTENT }
    val completionAttachments = allAttachments.filter { it.purpose == AttachPurposes.COMPLETION }

    var showDelete by remember { mutableStateOf(false) }
    var newSubtask by remember { mutableStateOf("") }

    val task = data?.task

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (task != null) {
                        IconButton(onClick = vm::togglePinned) {
                            Icon(
                                imageVector = if (task.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (task.pinned) "Unpin" else "Pin",
                                tint = if (task.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
                .padding(bottom = 96.dp),
        ) {
            if (task != null) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                DueChip(task, Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(8.dp))
                LabelChipsRow(labels, Modifier.padding(horizontal = 16.dp))

                val total = data?.totalCount ?: 0
                if (total > 0) {
                    Spacer(Modifier.height(12.dp))
                    SubtaskProgress(total, data?.doneCount ?: 0, Modifier.padding(horizontal = 16.dp))
                }

                if (task.notes.isNotBlank()) {
                    SectionHeader("Notes", Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                SectionHeader("Subtasks", Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                data?.subtasks?.forEach { sub ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    ) {
                        Checkbox(checked = sub.done, onCheckedChange = { vm.setSubtaskDone(sub, it) })
                        Text(
                            text = sub.title,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            color = if (sub.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = { vm.deleteSubtask(sub) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = newSubtask,
                        onValueChange = { newSubtask = it },
                        placeholder = { Text("Add subtask") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            vm.addSubtask(newSubtask)
                            newSubtask = ""
                        },
                        enabled = newSubtask.isNotBlank(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add subtask")
                    }
                }

                SectionHeader("Notes & media", Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                MediaPickerBar(
                    onImportUri = { uri ->
                        container.mediaImporter.importFromUri(uri, OwnerTypes.TASK, taskId, AttachPurposes.TASK_CONTENT) != null
                    },
                    onImportFile = { file, mime, name ->
                        container.mediaImporter.importFromFile(file, mime, name, OwnerTypes.TASK, taskId, AttachPurposes.TASK_CONTENT) != null
                    },
                )
                AttachedMediaGrid(
                    attachments = contentAttachments,
                    fileFor = container.attachmentRepo::fileFor,
                    onDelete = { attachment -> scope.launch { container.attachmentRepo.remove(attachment) } },
                )

                if (task.completed) {
                    SectionHeader("Completion", Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
                    RatingStars(task.completeRating ?: 0)
                    if (!task.completeReflection.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = task.completeReflection,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    if (task.completedAt != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Completed ${Dates.formatDateTime(task.completedAt)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    AttachedMediaGrid(
                        attachments = completionAttachments,
                        fileFor = container.attachmentRepo::fileFor,
                        onDelete = { attachment -> scope.launch { container.attachmentRepo.remove(attachment) } },
                    )
                    Spacer(Modifier.height(24.dp))
                    FilledTonalButton(
                        onClick = { vm.uncomplete() },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Text("Reopen task")
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                    FilledTonalButton(
                        onClick = {
                            if (settings?.ratingOnComplete == true) {
                                onComplete(task.id)
                            } else {
                                vm.complete(null, null)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Text("Mark done")
                    }
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "Delete task?",
            message = "This removes the task and its media permanently.",
            onConfirm = {
                showDelete = false
                vm.delete(onDeleted = { onBack() }, onDeleteOwner = container.attachmentRepo::deleteForOwner)
            },
            onDismiss = { showDelete = false },
        )
    }
}

@Composable
private fun RatingStars(rating: Int) {
    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = if (index < rating) Color(0xFFE6A23C) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}