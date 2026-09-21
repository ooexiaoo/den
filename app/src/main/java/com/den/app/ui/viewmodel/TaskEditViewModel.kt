package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.data.model.Subtask
import com.den.app.data.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskEditUi(
    val taskId: Long? = null,
    val loaded: Boolean = false,
    val title: String = "",
    val notes: String = "",
    val dueAt: Long? = null,
    val hasDue: Boolean = false,
    val reminderAt: Long? = null,
    val hasReminder: Boolean = false,
    val priority: Int = 0,
    val colorIndex: Int? = null,
    val pinned: Boolean = false,
    val selectedLabels: List<Long> = emptyList(),
    val subtasks: List<Subtask> = emptyList(),
    val removedSubtasks: List<Subtask> = emptyList(),
)

class TaskEditViewModel(
    container: AppContainer,
    taskId: Long?,
) : ViewModel() {

    private val taskRepo = container.taskRepo
    private val labelRepo = container.labelRepo

    private var editingTask: Task? = null

    val ui = MutableStateFlow(TaskEditUi(taskId = taskId))
    val allLabels = labelRepo.observeLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (taskId != null) {
            viewModelScope.launch {
                load(taskId)
            }
        } else {
            ui.update { it.copy(loaded = true) }
        }
    }

    private suspend fun load(id: Long) {
        val task = taskRepo.getTask(id) ?: return
        editingTask = task
        val subtasks = taskRepo.taskWithSubtasks(id)?.subtasks ?: emptyList()
        val labelIds = labelRepo.observeTaskLabels(id).first().map { it.id }
        ui.update {
            it.copy(
                loaded = true,
                title = task.title,
                notes = task.notes,
                dueAt = task.dueAt,
                hasDue = task.dueAt != null,
                reminderAt = task.reminderAt,
                hasReminder = task.reminderAt != null,
                priority = task.priority,
                colorIndex = task.colorIndex,
                pinned = task.pinned,
                selectedLabels = labelIds,
                subtasks = subtasks,
            )
        }
    }

    fun setTitle(value: String) = ui.update { it.copy(title = value) }
    fun setNotes(value: String) = ui.update { it.copy(notes = value) }

    fun onDueSet(timestamp: Long) = ui.update { it.copy(hasDue = true, dueAt = timestamp) }
    fun onDueClear() = ui.update { it.copy(hasDue = false, dueAt = null, hasReminder = false, reminderAt = null) }
    fun onReminderSet(timestamp: Long) = ui.update { it.copy(hasReminder = true, reminderAt = timestamp) }
    fun onReminderClear() = ui.update { it.copy(hasReminder = false, reminderAt = null) }

    fun setPriority(value: Int) = ui.update { it.copy(priority = value) }
    fun setColor(value: Int?) = ui.update { it.copy(colorIndex = value) }
    fun togglePin() = ui.update { it.copy(pinned = !it.pinned) }

    fun toggleLabel(labelId: Long) = ui.update {
        val labels = it.selectedLabels.toMutableList()
        if (labelId in labels) labels.remove(labelId) else labels.add(labelId)
        it.copy(selectedLabels = labels)
    }

    fun addSubtaskLocally(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        ui.update {
            val list = it.subtasks.toMutableList()
            list.add(Subtask(taskId = it.taskId ?: 0L, title = clean, sortOrder = list.size))
            it.copy(subtasks = list)
        }
    }

    fun toggleSubtaskDone(id: Long, done: Boolean) = ui.update {
        it.copy(subtasks = it.subtasks.map { sub -> if (sub.id == id) sub.copy(done = done) else sub })
    }

    fun removeSubtask(sub: Subtask) = ui.update {
        val removed = if (sub.id != 0L) it.removedSubtasks + sub else it.removedSubtasks
        it.copy(subtasks = it.subtasks.filterNot { s -> s.id == sub.id && s.title == sub.title }, removedSubtasks = removed)
    }

    fun reorderSubtask(id: Long, delta: Int) = ui.update {
        val list = it.subtasks.toMutableList()
        val index = list.indexOfFirst { s -> s.id == id }
        if (index < 0) return@update it
        val target = (index + delta).coerceIn(0, list.lastIndex)
        if (target == index) return@update it
        val item = list.removeAt(index)
        list.add(target, item)
        val reordered = list.mapIndexed { i, s -> s.copy(sortOrder = i) }
        it.copy(subtasks = reordered)
    }

    suspend fun save(): Long {
        val state = ui.value
        val cleanTitle = state.title.trim()
        val due = if (state.hasDue) state.dueAt else null
        val reminder = if (state.hasReminder) state.reminderAt else null

        val id = editingTask?.let { existing ->
            taskRepo.update(
                existing.copy(
                    title = cleanTitle,
                    notes = state.notes,
                    dueAt = due,
                    reminderAt = reminder,
                    priority = state.priority,
                    colorIndex = state.colorIndex,
                    pinned = state.pinned,
                )
            )
            existing.id
        } ?: taskRepo.create(
            title = cleanTitle,
            notes = state.notes,
            dueAt = due,
            reminderAt = reminder,
            priority = state.priority,
            colorIndex = state.colorIndex,
            pinned = state.pinned,
        )

        taskRepo.setLabels(id, state.selectedLabels)

        state.removedSubtasks.forEach { taskRepo.deleteSubtask(it) }

        state.subtasks.forEach { sub ->
            when {
                sub.id == 0L -> taskRepo.addSubtask(id, sub.title, sub.done)
                else -> taskRepo.updateSubtask(sub.copy(taskId = id))
            }
        }
        return id
    }
}