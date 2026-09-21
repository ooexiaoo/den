package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.data.model.AttachPurposes
import com.den.app.data.model.Attachment
import com.den.app.data.model.OwnerTypes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompletionViewModel(
    container: AppContainer,
    private val taskId: Long,
) : ViewModel() {

    private val taskRepo = container.taskRepo
    val completionAttachments: StateFlow<List<Attachment>> =
        container.attachmentRepo.observeForOwner(OwnerTypes.TASK, taskId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _rating = MutableStateFlow(0)
    val rating: StateFlow<Int> = _rating

    private val _reflection = MutableStateFlow("")
    val reflection: StateFlow<String> = _reflection

    fun setRating(value: Int) {
        _rating.value = value.coerceIn(0, 5)
    }

    fun setReflection(value: String) {
        _reflection.value = value
    }

    fun didComplete(): StateFlow<Boolean> = _completed
    private val _completed = MutableStateFlow(false)

    fun complete() {
        viewModelScope.launch {
            val task = taskRepo.getTask(taskId) ?: return@launch
            if (!task.completed) {
                taskRepo.toggleCompletion(
                    task,
                    rating = _rating.value.takeIf { it > 0 },
                    reflection = _reflection.value.trim().ifEmpty { null },
                )
            }
            _completed.value = true
        }
    }
}