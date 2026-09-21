package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.data.model.Label
import com.den.app.data.model.LabelWithCount
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LabelsViewModel(container: AppContainer) : ViewModel() {

    private val labelRepo = container.labelRepo

    val labels: StateFlow<List<LabelWithCount>> = labelRepo.observeLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, colorIndex: Int) {
        viewModelScope.launch { labelRepo.create(name, colorIndex) }
    }

    fun update(label: Label, name: String, colorIndex: Int) {
        viewModelScope.launch { labelRepo.update(label.copy(name = name, colorIndex = colorIndex)) }
    }

    fun delete(label: Label) {
        viewModelScope.launch { labelRepo.delete(label) }
    }
}