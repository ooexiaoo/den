package com.den.app.data.repo

import com.den.app.data.db.LabelDao
import com.den.app.data.model.Label
import com.den.app.data.model.LabelWithCount
import com.den.app.data.model.Note
import com.den.app.data.model.Task
import kotlinx.coroutines.flow.Flow

class LabelRepository(private val labelDao: LabelDao) {

    fun observeLabels(): Flow<List<LabelWithCount>> = labelDao.observeLabelRows()

    suspend fun all(): List<Label> = labelDao.all()

    suspend fun getLabel(id: Long): Label? = labelDao.getById(id)

    suspend fun create(name: String, colorIndex: Int): Long {
        val clean = name.trim()
        if (clean.isEmpty()) return -1L
        val existing = labelDao.all().firstOrNull { it.name.equals(clean, ignoreCase = true) }
        if (existing != null) return existing.id
        return labelDao.insert(Label(name = clean, colorIndex = colorIndex))
    }

    suspend fun update(label: Label) {
        labelDao.update(label.copy(name = label.name.trim()))
    }

    suspend fun delete(label: Label) {
        labelDao.deleteTaskCrossRefsForLabel(label.id)
        labelDao.deleteNoteCrossRefsForLabel(label.id)
        labelDao.delete(label)
    }

    fun observeTasksForLabel(labelId: Long): Flow<List<Task>> = labelDao.observeTasksForLabel(labelId)

    fun observeNotesForLabel(labelId: Long): Flow<List<Note>> = labelDao.observeNotesForLabel(labelId)

    fun observeTaskLabels(taskId: Long): Flow<List<Label>> = labelDao.observeTaskLabels(taskId)

    fun observeNoteLabels(noteId: Long): Flow<List<Label>> = labelDao.observeNoteLabels(noteId)
}