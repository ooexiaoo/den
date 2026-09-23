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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.den.app.AppContainer
import com.den.app.data.model.AttachPurposes
import com.den.app.data.model.OwnerTypes
import com.den.app.ui.components.AttachedMediaGrid
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.MediaPickerBar
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.CompletionViewModel
import kotlinx.coroutines.launch

@Composable
fun CompletionScreen(
    container: AppContainer,
    taskId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val vm: CompletionViewModel = viewModel(factory = DenViewModelFactory(container, id = taskId))
    val scope = rememberCoroutineScope()
    val rating by vm.rating.collectAsState()
    val reflection by vm.reflection.collectAsState()
    val attachments by vm.completionAttachments.collectAsState()
    val completed by vm.didComplete().collectAsState()
    val task by container.taskRepo.observeById(taskId).collectAsState(initial = null)

    LaunchedEffect(completed) {
        if (completed) onDone()
    }

    Scaffold(
        topBar = {
            DenTopBar(
                title = "Complete",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
            Text(
                text = "Nice work!",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = task?.title ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(24.dp))
            Text("How did it go?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) { index ->
                    IconButton(
                        onClick = { vm.setRating(if (rating == index + 1) 0 else index + 1) },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = if (index < rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Rate ${index + 1}",
                            tint = if (index < rating) Color(0xFFE6A23C) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = reflection,
                onValueChange = vm::setReflection,
                label = { Text("Reflection (optional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Text("Add a memory", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            MediaPickerBar(
                onImportUri = { uri ->
                    container.mediaImporter.importFromUri(uri, OwnerTypes.TASK, taskId, AttachPurposes.COMPLETION) != null
                },
                onImportFile = { file, mime, name ->
                    container.mediaImporter.importFromFile(file, mime, name, OwnerTypes.TASK, taskId, AttachPurposes.COMPLETION) != null
                },
            )
            AttachedMediaGrid(
                attachments = attachments,
                fileFor = container.attachmentRepo::fileFor,
                onDelete = { attachment -> scope.launch { container.attachmentRepo.remove(attachment) } },
            )

            Spacer(Modifier.height(28.dp))
            FilledTonalButton(
                onClick = { vm.complete() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Complete task")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}