package com.den.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.den.app.ui.components.DenTopBar

@Composable
fun MoreScreen(
    onOpenLabels: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRoadmap: (String) -> Unit,
) {
    Scaffold(
        topBar = { DenTopBar(title = "More") },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            MoreRow(
                icon = Icons.Filled.Inbox,
                title = "Inbox",
                subtitle = "One place for quick captures",
                onClick = { onOpenRoadmap(RoadmapSections.INBOX) },
            )
            Spacer(Modifier.height(2.dp))
            MoreRow(
                icon = Icons.Filled.Folder,
                title = "Projects",
                subtitle = "Group work toward an outcome",
                onClick = { onOpenRoadmap(RoadmapSections.PROJECTS) },
            )
            Spacer(Modifier.height(2.dp))
            MoreRow(
                icon = Icons.Filled.Hub,
                title = "Graph",
                subtitle = "See how everything connects",
                onClick = { onOpenRoadmap(RoadmapSections.GRAPH) },
            )
            Spacer(Modifier.height(2.dp))
            MoreRow(
                icon = Icons.Filled.People,
                title = "People",
                subtitle = "Link people to their work",
                onClick = { onOpenRoadmap(RoadmapSections.PEOPLE) },
            )
            Spacer(Modifier.height(2.dp))
            MoreRow(
                icon = Icons.Filled.EmojiEvents,
                title = "Goals",
                subtitle = "Track outcomes over time",
                onClick = { onOpenRoadmap(RoadmapSections.GOALS) },
            )
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp))
            MoreRow(
                icon = Icons.Filled.Label,
                title = "Labels",
                subtitle = "Group tasks and notes with colors",
                onClick = onOpenLabels,
            )
            Spacer(Modifier.height(2.dp))
            MoreRow(
                icon = Icons.Filled.Settings,
                title = "Settings",
                subtitle = "Theme, reminders, privacy and more",
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun MoreRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}