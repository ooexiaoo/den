package com.den.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.den.app.ui.viewmodel.TaskListItem
import com.den.app.util.Dates

private const val DAY_MILLIS = 86_400_000L

@Composable
private fun ContextMenuRow(
    label: String,
    icon: ImageVector,
    onAction: () -> Unit,
    tint: Color = Color.Unspecified,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAction)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
        )
    }
}

@Composable
private fun RescheduleChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** Context menu shown on long-press / swipe-schedule of a task row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskContextMenu(
    item: TaskListItem,
    onDismiss: () -> Unit,
    onToggleDone: () -> Unit,
    onEdit: () -> Unit,
    onReschedule: (Long) -> Unit,
    onClearDate: () -> Unit,
    onArchive: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onDelete: () -> Unit,
) {
    val today = Dates.startOfDay(Dates.now())
    val archived = onRestore != null
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(
                text = item.task.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            onRestore?.let {
                ContextMenuRow(label = "Restore", icon = Icons.Filled.Unarchive, onAction = it)
            }
            if (!archived) {
                if (item.task.completed) {
                    ContextMenuRow(label = "Reopen", icon = Icons.Filled.CheckCircle, onAction = onToggleDone)
                } else {
                    ContextMenuRow(label = "Mark done", icon = Icons.Filled.Check, onAction = onToggleDone)
                }
                ContextMenuRow(label = "Edit", icon = Icons.Filled.Edit, onAction = onEdit)
                onArchive?.let {
                    ContextMenuRow(label = "Archive", icon = Icons.Filled.Archive, onAction = it)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Text(
                    text = "Reschedule",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    RescheduleChip(label = "Today") { onReschedule(today) }
                    RescheduleChip(label = "Tomorrow") { onReschedule(today + DAY_MILLIS) }
                    RescheduleChip(label = "Next week") { onReschedule(today + 7 * DAY_MILLIS) }
                    if (item.task.dueAt != null) RescheduleChip(label = "No date") { onClearDate() }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            ContextMenuRow(
                label = "Delete",
                icon = Icons.Filled.Delete,
                tint = MaterialTheme.colorScheme.error,
                onAction = onDelete,
            )
        }
    }
}