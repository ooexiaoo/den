package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.data.model.Note
import com.den.app.data.model.OwnerTypes
import com.den.app.data.model.Task
import com.den.app.data.model.TaskWithSubtasks
import com.den.app.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(container: AppContainer) : ViewModel() {

    private val taskRepo = container.taskRepo
    private val noteRepo = container.noteRepo
    private val attachmentRepo = container.attachmentRepo

    private val allWithSubtasks: StateFlow<List<TaskWithSubtasks>> = taskRepo.observeAllWithSubtasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val notes: StateFlow<List<Note>> = noteRepo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val labelMap = noteRepo.observeNoteLabelJoins()
        .map { joins -> joins.groupBy({ it.noteId }, { it.label }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val backlinkCounts = noteRepo.observeBacklinkCounts()
        .map { rows -> rows.associate { it.id to it.cnt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val attachmentCounts = attachmentRepo.observeCountsByOwner(OwnerTypes.NOTE)
        .map { rows -> rows.associate { it.id to it.cnt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private fun toItem(row: TaskWithSubtasks) = TaskListItem(row.task, row.doneCount, row.totalCount)

    val todayRows: StateFlow<List<TaskListItem>> = allWithSubtasks
        .map { rows ->
            rows
                .asSequence()
                .filter { row ->
                    val t = row.task
                    !t.completed && t.dueAt != null && Dates.startOfDay(t.dueAt) <= Dates.startOfDay(Dates.now())
                }
                .sortedWith(
                    compareByDescending<TaskWithSubtasks> { it.task.pinned }
                        .thenBy { it.task.dueAt ?: Long.MAX_VALUE }
                        .thenByDescending { it.task.priority }
                        .thenByDescending { it.task.createdAt }
                )
                .map { toItem(it) }
                .toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val upcomingRows: StateFlow<List<TaskListItem>> = allWithSubtasks
        .map { rows ->
            rows
                .asSequence()
                .filter { row ->
                    val t = row.task
                    !t.completed && t.dueAt != null && Dates.startOfDay(t.dueAt) > Dates.startOfDay(Dates.now())
                }
                .sortedWith(
                    compareByDescending<TaskWithSubtasks> { it.task.pinned }
                        .thenBy { it.task.dueAt ?: Long.MAX_VALUE }
                        .thenByDescending { it.task.priority }
                )
                .map { toItem(it) }
                .take(4)
                .toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentNotes: StateFlow<List<NoteListItem>> =
        combine(notes, labelMap, backlinkCounts, attachmentCounts) { list, lm, bc, ac ->
            list
                .sortedByDescending { n -> n.updatedAt }
                .take(3)
                .map { n ->
                    NoteListItem(
                        note = n,
                        labels = lm[n.id] ?: emptyList(),
                        backlinkCount = bc[n.id] ?: 0,
                        attachmentCount = ac[n.id] ?: 0,
                    )
                }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dayProgress: StateFlow<Pair<Int, Int>> = allWithSubtasks
        .map { rows ->
            val dueToday = rows.filter { row ->
                row.task.dueAt != null && Dates.startOfDay(row.task.dueAt!!) == Dates.startOfDay(Dates.now())
            }
            dueToday.count { it.task.completed } to dueToday.size
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0 to 0)

    val openCount: StateFlow<Int> = allWithSubtasks
        .map { rows -> rows.count { !it.task.completed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun complete(task: Task) {
        viewModelScope.launch { taskRepo.toggleCompletion(task, null, null) }
    }

    fun uncomplete(task: Task) {
        viewModelScope.launch { taskRepo.uncomplete(task) }
    }

    suspend fun createNote(): Long = noteRepo.create(title = "", body = "")
}