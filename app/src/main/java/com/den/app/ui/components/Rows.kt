package com.den.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.den.app.data.model.Label
import com.den.app.data.model.Task
import com.den.app.util.Dates

@Composable
fun TaskRow(
    task: Task,
    labels: List<Label>,
    subtasksDone: Int,
    subtasksTotal: Int,
    onClick: () -> Unit,
    onCheck: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(modifier = Modifier.clickable(onClick = onCheck).padding(top = 2.dp)) {
                Icon(
                    imageVector = if (task.completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (task.completed) "Uncheck" else "Complete",
                    tint = if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (task.completed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    PriorityDot(task.priority, modifier = Modifier.padding(top = 6.dp))
                    if (task.pinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp).padding(top = 2.dp),
                        )
                    }
                }
                DueChip(task, modifier = Modifier.padding(top = 4.dp))
                if (subtasksTotal > 0) {
                    Spacer(Modifier.height(8.dp))
                    SubtaskProgress(subtasksTotal, subtasksDone)
                }
                if (labels.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LabelChipsRow(labels)
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: com.den.app.data.model.Note,
    labels: List<Label>,
    onClick: () -> Unit,
) {
    val accent = com.den.app.ui.theme.colorForIndex(note.colorIndex)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (accent == androidx.compose.ui.graphics.Color.Transparent) {
                MaterialTheme.colorScheme.surface
            } else {
                androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surface, accent, 0.06f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clickable(onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(
                        color = if (accent == androidx.compose.ui.graphics.Color.Transparent) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            accent.copy(alpha = 0.6f)
                        },
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    ),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title.ifBlank { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = if (note.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                    if (note.pinned) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (note.body.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = preview(note.body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (labels.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LabelChipsRow(labels)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = Dates.formatDateShort(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }
        }
    }
}

private fun preview(body: String): String {
    val clean = body
        .replace(Regex("\\[\\[[0-9]+:(.*?)]]"), "$1")
        .replace(Regex("^\\s*[-*]\\s+", RegexOption.MULTILINE), "")
        .trim()
    return clean
}