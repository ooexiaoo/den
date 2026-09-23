package com.den.app.data.repo

import com.den.app.data.db.LabelDao
import com.den.app.data.db.IdCount
import com.den.app.data.db.NoteDao
import com.den.app.data.db.NoteTitleRow
import com.den.app.data.model.Note
import com.den.app.data.model.NoteLabelCrossRef
import com.den.app.data.model.NoteLabelJoin
import com.den.app.data.model.OwnerTypes
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val labelDao: LabelDao,
) {
    fun observeActive(): Flow<List<Note>> = noteDao.observeActive()

    fun observeArchived(): Flow<List<Note>> = noteDao.observeArchived()

    fun observeById(id: Long): Flow<Note?> = noteDao.observeById(id)

    fun observeBacklinks(noteId: Long): Flow<List<Note>> =
        noteDao.observeBacklinks("[[$noteId:")

    fun observeBacklinkCounts(): Flow<List<IdCount>> = noteDao.observeBacklinkCounts()

    fun observeNoteLabelJoins(): Flow<List<NoteLabelJoin>> = labelDao.observeNoteLabelJoins()

    suspend fun titleRows(): List<NoteTitleRow> = noteDao.titleRows()

    suspend fun getNote(id: Long): Note? = noteDao.getById(id)

    suspend fun create(
        title: String = "",
        body: String = "",
        colorIndex: Int? = null,
        pinned: Boolean = false,
    ): Long {
        val now = System.currentTimeMillis()
        return noteDao.insert(Note(title = title, body = body, colorIndex = colorIndex, pinned = pinned, createdAt = now, updatedAt = now))
    }

    suspend fun update(note: Note) {
        noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun setPinned(note: Note, pinned: Boolean) {
        noteDao.setPinned(note.id, pinned, System.currentTimeMillis())
    }

    suspend fun setArchived(note: Note, archived: Boolean) {
        noteDao.setArchived(note.id, archived, System.currentTimeMillis())
    }

    suspend fun setLabels(noteId: Long, labelIds: List<Long>) {
        labelDao.deleteNoteCrossRefsForNote(noteId)
        if (labelIds.isNotEmpty()) {
            labelDao.insertNoteCrossRefs(labelIds.map { NoteLabelCrossRef(noteId, it) })
        }
    }

    suspend fun delete(note: Note, onDeleteOwner: suspend (String, Long) -> Unit) {
        onDeleteOwner(OwnerTypes.NOTE, note.id)
        noteDao.delete(note)
    }
}