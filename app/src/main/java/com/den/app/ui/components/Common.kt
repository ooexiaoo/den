package com.den.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.den.app.data.model.Label
import com.den.app.data.model.Task
import com.den.app.ui.theme.PALETTES
import com.den.app.ui.theme.colorForIndex
import com.den.app.util.Dates

val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun SubtaskProgress(subtasks: Int, done: Int, modifier: Modifier = Modifier) {
    val fraction = if (subtasks == 0) 0f else done.toFloat() / subtasks
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = if (done == subtasks && subtasks > 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "$done/$subtasks",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PriorityDot(priority: Int, modifier: Modifier = Modifier) {
    val color = when (priority) {
        3 -> MaterialTheme.colorScheme.error
        2 -> Color(0xFFE6A23C)
        1 -> MaterialTheme.colorScheme.tertiary
        else -> return
    }
    Box(
        modifier = modifier.size(8.dp).background(color, CircleShape)
    )
}

@Composable
fun DueChip(task: Task, modifier: Modifier = Modifier) {
    val due = task.dueAt ?: return
    val overdue = !task.completed && Dates.isOverdue(due)
    val color = when {
        task.completed -> MaterialTheme.colorScheme.onSurfaceVariant
        overdue -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val label = if (task.completed) {
        "Done ${Dates.humanDay(due, false)}"
    } else if (overdue) {
        "Overdue · ${Dates.humanDay(due, false)}"
    } else {
        Dates.humanDay(due, false)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
        if (task.reminderAt != null && !task.completed) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "alarm ${Dates.formatTime(task.reminderAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun LabelPill(label: Label, modifier: Modifier = Modifier) {
    val base = colorForIndex(label.colorIndex)
    val textColor = if (base == Color.Transparent) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        val lum = base.luminance()
        if (lum > 0.5f) Color(0xFF111111) else Color(0xFFFFFFFF)
    }
    Box(
        modifier = modifier
            .background(if (base == Color.Transparent) MaterialTheme.colorScheme.surfaceVariant else base.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(
            text = label.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (base == Color.Transparent) MaterialTheme.colorScheme.onSurfaceVariant else textColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun LabelChipsRow(labels: List<Label>, modifier: Modifier = Modifier) {
    if (labels.isEmpty()) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEach { LabelPill(it) }
    }
}

val paletteColors: List<Color> get() = PALETTES.map { it.seed }

data class ChipItem(
    val label: String,
    val selected: Boolean = false,
    val onClick: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterChipRow(
    items: List<ChipItem>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            FilterChip(
                selected = item.selected,
                onClick = item.onClick,
                label = { Text(item.label, maxLines = 1) },
            )
        }
    }
}