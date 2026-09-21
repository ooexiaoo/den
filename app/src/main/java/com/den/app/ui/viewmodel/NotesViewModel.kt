package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.data.model.Note
import com.den.app.data.model.OwnerTypes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(container: AppContainer) : ViewModel() {

    private val noteRepo = container.noteRepo

    val allNotes: StateFlow<List<Note>> = noteRepo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _search = MutableStateFlow("")
    private val _pinnedOnly = MutableStateFlow(false)

    val search: StateFlow<String> = _search
    val pinnedOnly: StateFlow<Boolean> = _pinnedOnly

    val visibleNotes: StateFlow<List<Note>> =
        combine(allNotes, _search, _pinnedOnly) { notes, query, onlyPinned ->
            val q = query.trim()
            notes
                .filter { n -> !onlyPinned || n.pinned }
                .filter { n -> q.isEmpty() || n.title.contains(q, ignoreCase = true) || n.body.contains(q, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearch(query: String) {
        _search.value = query
    }

    fun setPinnedOnly(value: Boolean) {
        _pinnedOnly.value = value
    }

    fun togglePinned(note: Note) {
        viewModelScope.launch { noteRepo.setPinned(note, !note.pinned) }
    }

    fun delete(note: Note) {
        viewModelScope.launch { noteRepo.delete(note, onDeleteOwner = { _, _ -> }) }
    }

    suspend fun createDraft(): Long = noteRepo.create(title = "", body = "")
}