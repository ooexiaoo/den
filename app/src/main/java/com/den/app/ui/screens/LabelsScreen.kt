package com.den.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.den.app.AppContainer
import com.den.app.data.model.Label
import com.den.app.data.model.LabelWithCount
import com.den.app.ui.components.ConfirmDialog
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.EmptyState
import com.den.app.ui.components.LabelEditorDialog
import com.den.app.ui.theme.colorForIndex
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.LabelsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelsScreen(
    container: AppContainer,
    onOpenLabel: (Long) -> Unit,
) {
    val vm: LabelsViewModel = viewModel(factory = DenViewModelFactory(container))
    val labels by vm.labels.collectAsState()

    var editor by remember { mutableStateOf<Label?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Label?>(null) }
    var menuFor by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            DenTopBar(
                title = "Labels",
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNew = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New label")
            }
        },
    ) { padding ->
        if (labels.isEmpty()) {
            EmptyState(
                title = "No labels yet",
                subtitle = "Add labels to organize tasks and notes",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            ) {
                items(labels, key = { it.label.id }) { row ->
                    LabelRow(
                        row = row,
                        menuOpen = menuFor == row.label.id,
                        onClick = { onOpenLabel(row.label.id) },
                        onMenuToggle = { menuFor = if (menuFor == row.label.id) null else row.label.id },
                        onEdit = { editor = row.label; menuFor = null },
                        onDelete = { deleting = row.label; menuFor = null },
                    )
                }
            }
        }
    }

    if (showNew) {
        LabelEditorDialog(
            editing = null,
            onSave = { name, color -> vm.create(name, color) },
            onDismiss = { showNew = false },
        )
    }
    editor?.let { label ->
        LabelEditorDialog(
            editing = label,
            onSave = { name, color -> vm.update(label, name, color) },
            onDismiss = { editor = null },
        )
    }
    deleting?.let { label ->
        ConfirmDialog(
            title = "Delete label?",
            message = "The label \"${label.name}\" will be removed from all items.",
            onConfirm = {
                vm.delete(label)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun LabelRow(
    row: LabelWithCount,
    menuOpen: Boolean,
    onClick: () -> Unit,
    onMenuToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val accent = colorForIndex(row.label.colorIndex)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                Modifier
                    .size(14.dp)
                    .background(if (accent == Color.Transparent) MaterialTheme.colorScheme.outlineVariant else accent, CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = row.label.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "${row.totalCount} item${if (row.totalCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onMenuToggle) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = onMenuToggle) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                onClick = onEdit,
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = onDelete,
            )
        }
    }
}