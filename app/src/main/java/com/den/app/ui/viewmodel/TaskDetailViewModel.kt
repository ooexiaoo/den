package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.data.model.AttachPurposes
import com.den.app.data.model.Attachment
import com.den.app.data.model.OwnerTypes
import com.den.app.data.model.Subtask
import com.den.app.data.model.TaskWithSubtasks
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskDetailViewModel(
    container: AppContainer,
    private val taskId: Long,
) : ViewModel() {

    private val taskRepo = container.taskRepo

    val withSubtasks: StateFlow<TaskWithSubtasks?> = taskRepo.observeWithSubtasks(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val labels = container.labelRepo.observeTaskLabels(taskId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val completionAttachments: StateFlow<List<Attachment>> =
        container.attachmentRepo.observeForOwner(OwnerTypes.TASK, taskId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun complete(rating: Int?, reflection: String?) {
        viewModelScope.launch {
            val task = taskRepo.getTask(taskId) ?: return@launch
            if (!task.completed) taskRepo.toggleCompletion(task, rating, reflection)
        }
    }

    fun uncomplete() {
        viewModelScope.launch {
            val task = taskRepo.getTask(taskId) ?: return@launch
            if (task.completed) taskRepo.uncomplete(task)
        }
    }

    fun addSubtask(title: String) {
        viewModelScope.launch { taskRepo.addSubtask(taskId, title) }
    }

    fun setSubtaskDone(subtask: Subtask, done: Boolean) {
        viewModelScope.launch { taskRepo.setSubtaskDone(subtask, done) }
    }

    fun deleteSubtask(subtask: Subtask) {
        viewModelScope.launch { taskRepo.deleteSubtask(subtask) }
    }

    fun togglePinned() {
        viewModelScope.launch {
            taskRepo.getTask(taskId)?.let { taskRepo.setPinned(it, !it.pinned) }
        }
    }

    fun delete(onDeleted: suspend () -> Unit, onDeleteOwner: suspend (String, Long) -> Unit) {
        viewModelScope.launch {
            val task = taskRepo.getTask(taskId) ?: return@launch
            taskRepo.delete(task, onDeleteOwner)
            onDeleted()
        }
    }
}