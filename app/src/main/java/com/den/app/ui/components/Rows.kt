package com.den.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.den.app.data.model.Label
import com.den.app.data.model.Task

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: Task,
    labels: List<Label>,
    subtasksDone: Int,
    subtasksTotal: Int,
    onClick: () -> Unit,
    onCheck: () -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    TaskRowContent(
        task = task,
        labels = labels,
        subtasksDone = subtasksDone,
        subtasksTotal = subtasksTotal,
        onCheck = onCheck,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
    )
}

@Composable
fun TaskRowContent(
    task: Task,
    labels: List<Label>,
    subtasksDone: Int,
    subtasksTotal: Int,
    onCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(modifier = Modifier.clickable(onClick = onCheck).padding(top = 2.dp)) {
            Icon(
                imageVector = if (task.completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (task.completed) "Uncheck" else "Complete",
                tint = if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
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
                PriorityDot(task.priority, modifier = Modifier.padding(top = 8.dp))
                if (task.pinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp).padding(top = 8.dp),
                    )
                }
            }
            DueChip(task, modifier = Modifier.padding(top = 4.dp))
            if (task.reminderAt != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Reminder",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = com.den.app.util.Dates.formatTime(task.reminderAt!!),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (subtasksTotal > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$subtasksDone/$subtasksTotal subtasks",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (labels.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LabelChipsRow(labels, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun NoteCard(
    note: com.den.app.data.model.Note,
    labels: List<Label>,
    backlinks: Int = 0,
    attachments: Int = 0,
    onClick: () -> Unit,
) {
    val accent = com.den.app.ui.theme.colorForIndex(note.colorIndex)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (accent == Color.Transparent) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surfaceContainerHigh, accent, 0.07f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                        color = if (accent == Color.Transparent) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = com.den.app.util.Dates.formatDateShort(note.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    val meta = buildList {
                        if (backlinks > 0) add("$backlinks backlink${if (backlinks == 1) "" else "s"}")
                        if (attachments > 0) add("$attachments attachment${if (attachments == 1) "" else "s"}")
                    }.joinToString("  ·  ")
                    if (meta.isNotEmpty()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                }
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