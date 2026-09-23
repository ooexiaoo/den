package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.den.app.AppContainer

class DenViewModelFactory(
    private val container: AppContainer,
    private val id: Long? = null,
    private val labelId: Long? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val viewModel: ViewModel = when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(container)
            modelClass.isAssignableFrom(TasksViewModel::class.java) -> TasksViewModel(container)
            modelClass.isAssignableFrom(TaskEditViewModel::class.java) -> TaskEditViewModel(container, id)
            modelClass.isAssignableFrom(TaskDetailViewModel::class.java) -> TaskDetailViewModel(container, id ?: -1L)
            modelClass.isAssignableFrom(CompletionViewModel::class.java) -> CompletionViewModel(container, id ?: -1L)
            modelClass.isAssignableFrom(NotesViewModel::class.java) -> NotesViewModel(container)
            modelClass.isAssignableFrom(NoteEditViewModel::class.java) -> NoteEditViewModel(container, id)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container)
            modelClass.isAssignableFrom(LabelsViewModel::class.java) -> LabelsViewModel(container)
            modelClass.isAssignableFrom(LabelDetailViewModel::class.java) -> LabelDetailViewModel(container, labelId ?: -1L)
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
        return viewModel as T
    }
}