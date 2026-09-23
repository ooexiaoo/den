package com.den.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.EmptyState

object RoadmapSections {
    const val INBOX = "inbox"
    const val PROJECTS = "projects"
    const val GRAPH = "graph"
    const val PEOPLE = "people"
    const val GOALS = "goals"
}

private data class RoadmapInfo(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val ROADMAPS = mapOf(
    RoadmapSections.INBOX to RoadmapInfo(
        icon = Icons.Filled.Inbox,
        title = "Inbox",
        description = "Quick capture that lands everything in one place, then gets sorted into your system.",
    ),
    RoadmapSections.PROJECTS to RoadmapInfo(
        icon = Icons.Filled.Folder,
        title = "Projects",
        description = "Group tasks, notes and files under shared goals with progress tracking.",
    ),
    RoadmapSections.GRAPH to RoadmapInfo(
        icon = Icons.Filled.Hub,
        title = "Graph",
        description = "A pan-and-zoom visualization of how your notes and tasks relate to each other.",
    ),
    RoadmapSections.PEOPLE to RoadmapInfo(
        icon = Icons.Filled.People,
        title = "People",
        description = "Link @people to the projects, tasks and meetings they are a part of.",
    ),
    RoadmapSections.GOALS to RoadmapInfo(
        icon = Icons.Filled.EmojiEvents,
        title = "Goals",
        description = "Long-term objectives that tie projects, tasks and habits together.",
    ),
)

@Composable
fun RoadmapScreen(
    slug: String,
    onBack: () -> Unit,
) {
    val info = ROADMAPS[slug] ?: return
    Scaffold(
        topBar = {
            DenTopBar(
                title = info.title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            EmptyState(
                icon = info.icon,
                title = "Coming soon",
                subtitle = info.description,
            )
        }
    }
}