package com.den.app.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val app: String = "den",
    val formatVersion: Int = 1,
    val createdAt: Long,
    val tasks: List<TaskDto> = emptyList(),
    val subtasks: List<SubtaskDto> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
    val labels: List<LabelDto> = emptyList(),
    val taskLabels: List<TaskLabelDto> = emptyList(),
    val noteLabels: List<NoteLabelDto> = emptyList(),
    val attachments: List<AttachmentDto> = emptyList(),
)

@Serializable
data class TaskDto(
    val id: Long,
    val title: String,
    val notes: String = "",
    val dueAt: Long? = null,
    val reminderAt: Long? = null,
    val completed: Boolean = false,
    val completeRating: Int? = null,
    val completeReflection: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val archived: Boolean = false,
    val pinned: Boolean = false,
    val colorIndex: Int? = null,
    val priority: Int = 0,
    val sortOrder: Long = 0L,
)

@Serializable
data class SubtaskDto(
    val id: Long,
    val taskId: Long,
    val title: String,
    val done: Boolean = false,
    val sortOrder: Int = 0,
)

@Serializable
data class NoteDto(
    val id: Long,
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val colorIndex: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class LabelDto(
    val id: Long,
    val name: String,
    val colorIndex: Int,
    val createdAt: Long,
)

@Serializable
data class TaskLabelDto(val taskId: Long, val labelId: Long)

@Serializable
data class NoteLabelDto(val noteId: Long, val labelId: Long)

@Serializable
data class AttachmentDto(
    val id: Long,
    val ownerType: String,
    val ownerId: Long,
    val purpose: String,
    val kind: String,
    val storedName: String,
    val displayName: String,
    val mime: String,
    val size: Long = 0L,
    val createdAt: Long,
)