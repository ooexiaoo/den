package com.den.app.port

import android.content.Context
import android.net.Uri
import com.den.app.data.db.DenDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PortPayload(
    val app: String = "Den",
    val exportedAt: Long = System.currentTimeMillis(),
    val labels: List<PortLabel> = emptyList(),
    val tasks: List<PortTask> = emptyList(),
    val notes: List<PortNote> = emptyList(),
)

@Serializable
data class PortLabel(val name: String, val colorIndex: Int)

@Serializable
data class PortTask(
    val title: String,
    val notes: String,
    val dueAt: Long?,
    val reminderAt: Long?,
    val completed: Boolean,
    val completeRating: Int?,
    val completeReflection: String?,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val archived: Boolean,
    val pinned: Boolean,
    val colorIndex: Int?,
    val priority: Int,
    val sortOrder: Long,
    val subtasks: List<PortSubtask>,
    val labelNames: List<String>,
)

@Serializable
data class PortSubtask(val title: String, val done: Boolean, val sortOrder: Int)

@Serializable
data class PortNote(
    val title: String,
    val body: String,
    val pinned: Boolean,
    val archived: Boolean,
    val colorIndex: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val labelNames: List<String>,
)

data class ImportSummary(val tasks: Int, val notes: Int, val labels: Int, val subtasks: Int)

class PortManager(
    private val context: Context,
    private val db: DenDatabase,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun exportJson(): String {
        val now = System.currentTimeMillis()
        val payload = PortPayload(
            exportedAt = now,
            labels = db.labelDao().all().map { PortLabel(it.name, it.colorIndex) },
            tasks = db.taskDao().allForBackup().map { task ->
                PortTask(
                    title = task.title,
                    notes = task.notes,
                    dueAt = task.dueAt,
                    reminderAt = task.reminderAt,
                    completed = task.completed,
                    completeRating = task.completeRating,
                    completeReflection = task.completeReflection,
                    completedAt = task.completedAt,
                    createdAt = task.createdAt,
                    updatedAt = task.updatedAt,
                    archived = task.archived,
                    pinned = task.pinned,
                    colorIndex = task.colorIndex,
                    priority = task.priority,
                    sortOrder = task.sortOrder,
                    subtasks = db.subtaskDao().listForTask(task.id).map {
                        PortSubtask(it.title, it.done, it.sortOrder)
                    },
                    labelNames = db.labelDao().labelsForTask(task.id).map { it.name },
                )
            },
            notes = db.noteDao().allForBackup().map { note ->
                PortNote(
                    title = note.title,
                    body = note.body,
                    pinned = note.pinned,
                    archived = note.archived,
                    colorIndex = note.colorIndex,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                    labelNames = db.labelDao().labelsForNote(note.id).map { it.name },
                )
            },
        )
        return json.encodeToString(PortPayload.serializer(), payload)
    }

    fun importJson(text: String): ImportSummary {
        val payload = json.decodeFromString<PortPayload>(text)

        val labelDao = db.labelDao()
        val existing = labelDao.all().associateBy { it.name.trim().lowercase() }
        val labelIdByName = mutableMapOf<String, Long>()
        payload.labels.forEach { label ->
            val key = label.name.trim().lowercase()
            if (key.isNotEmpty()) {
                val id = existing[key]?.id ?: labelDao.insert(
                    com.den.app.data.model.Label(name = label.name.trim(), colorIndex = label.colorIndex)
                )
                labelIdByName[key] = id
            }
        }

        var subtaskCount = 0
        payload.tasks.forEach { task ->
            val taskId = db.taskDao().insert(
                com.den.app.data.model.Task(
                    title = task.title,
                    notes = task.notes,
                    dueAt = task.dueAt,
                    reminderAt = task.reminderAt,
                    completed = task.completed,
                    completeRating = task.completeRating,
                    completeReflection = task.completeReflection,
                    completedAt = task.completedAt,
                    createdAt = task.createdAt,
                    updatedAt = task.updatedAt,
                    archived = task.archived,
                    pinned = task.pinned,
                    colorIndex = task.colorIndex,
                    priority = task.priority,
                    sortOrder = task.sortOrder,
                )
            )
            if (task.subtasks.isNotEmpty()) {
                db.subtaskDao().insertAll(
                    task.subtasks.map {
                        com.den.app.data.model.Subtask(
                            taskId = taskId,
                            title = it.title,
                            done = it.done,
                            sortOrder = it.sortOrder,
                        )
                    }
                )
                subtaskCount += task.subtasks.size
            }
            val labelIds = task.labelNames.mapNotNull { labelIdByName[it.trim().lowercase()] }
            if (labelIds.isNotEmpty()) {
                labelDao.insertTaskCrossRefs(labelIds.map { com.den.app.data.model.TaskLabelCrossRef(taskId, it) })
            }
        }

        payload.notes.forEach { note ->
            val noteId = db.noteDao().insert(
                com.den.app.data.model.Note(
                    title = note.title,
                    body = note.body,
                    pinned = note.pinned,
                    archived = note.archived,
                    colorIndex = note.colorIndex,
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt,
                )
            )
            val labelIds = note.labelNames.mapNotNull { labelIdByName[it.trim().lowercase()] }
            if (labelIds.isNotEmpty()) {
                labelDao.insertNoteCrossRefs(labelIds.map { com.den.app.data.model.NoteLabelCrossRef(noteId, it) })
            }
        }

        return ImportSummary(
            tasks = payload.tasks.size,
            notes = payload.notes.size,
            labels = labelIdByName.size,
            subtasks = subtaskCount,
        )
    }

    suspend fun exportToUri(uri: Uri): Result<Unit> {
        return runCatching {
            val out = context.contentResolver.openOutputStream(uri) ?: error("Cannot open destination")
            out.use { it.write(exportJson().toByteArray(Charsets.UTF_8)) }
        }
    }

    suspend fun importFromUri(uri: Uri): Result<ImportSummary> {
        return runCatching {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Cannot open file")
            importJson(text)
        }
    }
}