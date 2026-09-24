package com.den.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.den.app.AppContainer
import com.den.app.ui.components.DenCard
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.EmptyState
import com.den.app.ui.viewmodel.DenViewModelFactory
import com.den.app.ui.viewmodel.ReviewViewModel
import com.den.app.util.Dates

@Composable
fun ReviewScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val vm: ReviewViewModel = viewModel(factory = DenViewModelFactory(container))
    val stats by vm.stats.collectAsState()

    Scaffold(
        topBar = {
            DenTopBar(
                title = "Review",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                Text(
                    text = "Daily check-in",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "A quick look at what needs attention right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
            if (stats.dueToday.isNotEmpty()) {
                item { ReviewSection(icon = Icons.Filled.Today, title = "Due today", count = stats.dueToday.size, accent = MaterialTheme.colorScheme.primary) }
                items(stats.dueToday) { row -> ReviewTaskRow(row.task.title, row.task.dueAt) { onOpenTask(row.task.id) } }
                item { Spacer(Modifier.height(12.dp)) }
            }
            if (stats.overdue.isNotEmpty()) {
                item { ReviewSection(icon = Icons.Filled.Flag, title = "Overdue", count = stats.overdue.size, accent = MaterialTheme.colorScheme.error) }
                items(stats.overdue) { row -> ReviewTaskRow(row.task.title, row.task.dueAt) { onOpenTask(row.task.id) } }
                item { Spacer(Modifier.height(12.dp)) }
            }
            if (stats.upNext.isNotEmpty()) {
                item { ReviewSection(icon = Icons.Filled.Schedule, title = "Next 7 days", count = stats.upNext.size, accent = MaterialTheme.colorScheme.tertiary) }
                items(stats.upNext) { row -> ReviewTaskRow(row.task.title, row.task.dueAt) { onOpenTask(row.task.id) } }
                item { Spacer(Modifier.height(12.dp)) }
            }
            if (stats.noDate.isNotEmpty()) {
                item { ReviewSection(icon = Icons.Outlined.Inbox, title = "No date set", count = stats.noDate.size, accent = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(stats.noDate) { row -> ReviewTaskRow(row.task.title, row.task.dueAt) { onOpenTask(row.task.id) } }
                item { Spacer(Modifier.height(12.dp)) }
            }
            item {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp))
                Spacer(Modifier.height(8.dp))
            }
            item {
                DenCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.TaskAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Completed today · ${stats.completedToday.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (stats.completedToday.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Nothing closed out yet. Take one small win.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            item {
                DenCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${stats.noteCount} active note${if (stats.noteCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            if (stats.overdue.isEmpty() && stats.dueToday.isEmpty() && stats.noDate.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Today,
                        title = "Nothing pressing",
                        subtitle = "No overdue or due tasks right now. Enjoy it.",
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewSection(
    icon: ImageVector,
    title: String,
    count: Int,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ReviewTaskRow(
    title: String,
    dueAt: Long?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
        dueAt?.let {
            Text(
                text = Dates.formatDateShort(it),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}