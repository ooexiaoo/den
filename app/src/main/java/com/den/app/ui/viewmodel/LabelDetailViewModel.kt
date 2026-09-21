package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.data.model.Label
import com.den.app.data.model.Note
import com.den.app.data.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LabelDetailViewModel(
    container: AppContainer,
    private val labelId: Long,
) : ViewModel() {

    private val labelRepo = container.labelRepo

    private val _label = MutableStateFlow<Label?>(null)
    val label: StateFlow<Label?> = _label

    val tasks: StateFlow<List<Task>> = labelRepo.observeTasksForLabel(labelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<Note>> = labelRepo.observeNotesForLabel(labelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { _label.value = labelRepo.getLabel(labelId) }
    }
}