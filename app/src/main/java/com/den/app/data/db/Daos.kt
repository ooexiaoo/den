package com.den.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.den.app.data.model.Attachment
import com.den.app.data.model.Label
import com.den.app.data.model.LabelWithCount
import com.den.app.data.model.Note
import com.den.app.data.model.NoteLabelCrossRef
import com.den.app.data.model.Subtask
import com.den.app.data.model.Task
import com.den.app.data.model.TaskLabelCrossRef
import com.den.app.data.model.TaskLabelJoin
import com.den.app.data.model.TaskWithSubtasks
import kotlinx.coroutines.flow.Flow

data class NoteTitleRow(
    @androidx.room.ColumnInfo(name = "id") val id: Long,
    @androidx.room.ColumnInfo(name = "title") val title: String,
)

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE archived = 0 ORDER BY pinned DESC, completed ASC, title COLLATE NOCASE ASC")
    fun observeActive(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE archived = 0 AND completed = 0 AND reminderAt IS NOT NULL")
    suspend fun tasksWithReminders(): List<Task>

    @Query("SELECT * FROM tasks ORDER BY id ASC")
    suspend fun allForBackup(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Insert
    suspend fun insertAll(tasks: List<Task>)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTaskWithSubtasks(id: Long): Flow<TaskWithSubtasks?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE archived = 0 ")
    fun observeAllWithSubtasks(): Flow<List<TaskWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskWithSubtasks(id: Long): TaskWithSubtasks?

    @Query(
        """
        UPDATE tasks SET completed = :completed, completeRating = :rating,
        completeReflection = :reflection, completedAt = :completedAt, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun setCompletion(
        id: Long,
        completed: Boolean,
        rating: Int?,
        reflection: String?,
        completedAt: Long?,
        updatedAt: Long,
    )

    @Query("UPDATE tasks SET pinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET archived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, updatedAt: Long)

    @Query("DELETE FROM tasks WHERE archived = 1")
    suspend fun purgeArchived()
}

@Dao
interface SubtaskDao {

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, id ASC")
    fun observeForTask(taskId: Long): Flow<List<Subtask>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, id ASC")
    suspend fun listForTask(taskId: Long): List<Subtask>

    @Query("SELECT * FROM subtasks ORDER BY id ASC")
    suspend fun listAll(): List<Subtask>

    @Insert
    suspend fun insert(subtask: Subtask): Long

    @Insert
    suspend fun insertAll(subtasks: List<Subtask>)

    @Update
    suspend fun update(subtask: Subtask)

    @Delete
    suspend fun delete(subtask: Subtask)

    @Query("UPDATE subtasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)

    @Query("DELETE FROM subtasks")
    suspend fun clearAll()
}

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE archived = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun observeActive(): Flow<List<Note>>

    @Query("SELECT id, title FROM notes WHERE archived = 0 ORDER BY title COLLATE NOCASE ASC")
    suspend fun titleRows(): List<NoteTitleRow>

    @Query("SELECT * FROM notes ORDER BY id ASC")
    suspend fun allForBackup(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE archived = 0 AND body LIKE '%' || :token || '%' ORDER BY updatedAt DESC")
    fun observeBacklinks(token: String): Flow<List<Note>>

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Insert
    suspend fun insertAll(notes: List<Note>)

    @Query("DELETE FROM notes")
    suspend fun clearAll()

    @Query("UPDATE notes SET pinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET archived = :archived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, updatedAt: Long)
}

@Dao
interface LabelDao {

    @Query(
        """
        SELECT l.*,
            (SELECT COUNT(*) FROM task_labels tl WHERE tl.labelId = l.id) +
            (SELECT COUNT(*) FROM note_labels nl WHERE nl.labelId = l.id) AS totalCount
        FROM labels l ORDER BY l.name COLLATE NOCASE ASC
        """
    )
    fun observeLabelRows(): Flow<List<LabelWithCount>>

    @Query("SELECT * FROM labels ORDER BY name COLLATE NOCASE ASC")
    suspend fun allForBackup(): List<Label>

    @Query("SELECT * FROM labels ORDER BY name COLLATE NOCASE ASC")
    suspend fun all(): List<Label>

    @Query("SELECT * FROM labels WHERE id = :id")
    suspend fun getById(id: Long): Label?

    @Insert
    suspend fun insert(label: Label): Long

    @Update
    suspend fun update(label: Label)

    @Delete
    suspend fun delete(label: Label)

    @Insert
    suspend fun insertAll(labels: List<Label>)

    @Query("DELETE FROM labels")
    suspend fun clearAll()

    @Insert
    suspend fun insertTaskCrossRefs(crossRefs: List<TaskLabelCrossRef>)

    @Insert
    suspend fun insertNoteCrossRefs(crossRefs: List<NoteLabelCrossRef>)

    @Delete
    suspend fun deleteTaskCrossRef(crossRef: TaskLabelCrossRef)

    @Delete
    suspend fun deleteNoteCrossRef(crossRef: NoteLabelCrossRef)

    @Query("SELECT * FROM task_labels ORDER BY taskId")
    suspend fun allTaskCrossRefs(): List<TaskLabelCrossRef>

    @Query("SELECT * FROM note_labels ORDER BY noteId")
    suspend fun allNoteCrossRefs(): List<NoteLabelCrossRef>

    @Query("DELETE FROM task_labels WHERE labelId = :labelId")
    suspend fun deleteTaskCrossRefsForLabel(labelId: Long)

    @Query("DELETE FROM note_labels WHERE labelId = :labelId")
    suspend fun deleteNoteCrossRefsForLabel(labelId: Long)

    @Query("DELETE FROM task_labels WHERE taskId = :taskId")
    suspend fun deleteTaskCrossRefsForTask(taskId: Long)

    @Query("DELETE FROM note_labels WHERE noteId = :noteId")
    suspend fun deleteNoteCrossRefsForNote(noteId: Long)

    @Query("DELETE FROM task_labels")
    suspend fun clearTaskCrossRefs()

    @Query("DELETE FROM note_labels")
    suspend fun clearNoteCrossRefs()

    @Query(
        "SELECT l.* FROM labels l INNER JOIN task_labels tl ON tl.labelId = l.id " +
            "WHERE tl.taskId = :taskId ORDER BY l.name COLLATE NOCASE ASC"
    )
    fun observeTaskLabels(taskId: Long): Flow<List<Label>>

    @Query(
        "SELECT tl.taskId AS taskId, l.* FROM labels l INNER JOIN task_labels tl ON tl.labelId = l.id " +
            "ORDER BY l.name COLLATE NOCASE ASC"
    )
    fun observeTaskLabelJoins(): Flow<List<TaskLabelJoin>>

    @Query(
        "SELECT l.* FROM labels l INNER JOIN task_labels tl ON tl.labelId = l.id " +
            "WHERE tl.taskId = :taskId ORDER BY l.name COLLATE NOCASE ASC"
    )
    suspend fun labelsForTask(taskId: Long): List<Label>

    @Query(
        "SELECT l.* FROM labels l INNER JOIN note_labels nl ON nl.labelId = l.id " +
            "WHERE nl.noteId = :noteId ORDER BY l.name COLLATE NOCASE ASC"
    )
    fun observeNoteLabels(noteId: Long): Flow<List<Label>>

    @Query(
        "SELECT l.* FROM labels l INNER JOIN note_labels nl ON nl.labelId = l.id " +
            "WHERE nl.noteId = :noteId ORDER BY l.name COLLATE NOCASE ASC"
    )
    suspend fun labelsForNote(noteId: Long): List<Label>

    @Query(
        "SELECT t.* FROM tasks t INNER JOIN task_labels tl ON tl.taskId = t.id " +
            "WHERE tl.labelId = :labelId AND t.archived = 0 " +
            "ORDER BY t.completed ASC, t.title COLLATE NOCASE ASC"
    )
    fun observeTasksForLabel(labelId: Long): Flow<List<Task>>

    @Query(
        "SELECT n.* FROM notes n INNER JOIN note_labels nl ON nl.noteId = n.id " +
            "WHERE nl.labelId = :labelId AND n.archived = 0 ORDER BY n.pinned DESC, n.updatedAt DESC"
    )
    fun observeNotesForLabel(labelId: Long): Flow<List<Note>>
}

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY id DESC")
    fun observeForOwner(ownerType: String, ownerId: Long): Flow<List<Attachment>>

    @Query("SELECT * FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId ORDER BY id DESC")
    suspend fun listForOwner(ownerType: String, ownerId: Long): List<Attachment>

    @Query("SELECT * FROM attachments ORDER BY id DESC")
    suspend fun allForBackup(): List<Attachment>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: Long): Attachment?

    @Insert
    suspend fun insert(attachment: Attachment): Long

    @Insert
    suspend fun insertAll(attachments: List<Attachment>)

    @Delete
    suspend fun delete(attachment: Attachment)

    @Query("DELETE FROM attachments WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteForOwner(ownerType: String, ownerId: Long)

    @Query("DELETE FROM attachments")
    suspend fun clearAll()
}