package com.den.app.data.model

import androidx.room.Entity
import androidx.room.Embedded
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val dueAt: Long? = null,
    val reminderAt: Long? = null,
    val completed: Boolean = false,
    val completeRating: Int? = null,
    val completeReflection: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
    val pinned: Boolean = false,
    val colorIndex: Int? = null,
    val priority: Int = 0,
    val sortOrder: Long = 0L,
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(entity = Task::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("taskId")]
)
data class Subtask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val title: String,
    val done: Boolean = false,
    val sortOrder: Int = 0,
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val colorIndex: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "labels")
data class Label(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorIndex: Int,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "task_labels", primaryKeys = ["taskId", "labelId"])
data class TaskLabelCrossRef(
    val taskId: Long,
    val labelId: Long,
)

@Entity(tableName = "note_labels", primaryKeys = ["noteId", "labelId"])
data class NoteLabelCrossRef(
    val noteId: Long,
    val labelId: Long,
)

@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val ownerType: String,
    val ownerId: Long,
    val purpose: String,
    val kind: String,
    val storedName: String,
    val displayName: String,
    val mime: String,
    val size: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
)

data class TaskWithSubtasks(
    @Embedded
    val task: Task,
    @Relation(parentColumn = "id", entityColumn = "taskId")
    val subtasks: List<Subtask> = emptyList(),
) {
    val doneCount: Int get() = subtasks.count { it.done }
    val totalCount: Int get() = subtasks.size
}

data class LabelWithCount(
    @Embedded
    val label: Label,
    val totalCount: Int,
)

object OwnerTypes {
    const val TASK = "task"
    const val NOTE = "note"
}

object AttachPurposes {
    const val TASK_CONTENT = "task_content"
    const val COMPLETION = "completion"
    const val NOTE = "note"
}

object AttachKinds {
    const val IMAGE = "image"
    const val VIDEO = "video"
    const val AUDIO = "audio"
    const val FILE = "file"
}