package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.data.model.Label
import com.den.app.data.model.Note
import com.den.app.data.model.OwnerTypes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NoteFilter { ALL, PINNED, ARCHIVED }

data class NoteListItem(
    val note: Note,
    val labels: List<Label>,
    val backlinkCount: Int,
    val attachmentCount: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(container: AppContainer) : ViewModel() {

    private val noteRepo = container.noteRepo
    private val attachmentRepo = container.attachmentRepo

    private val allNotes: StateFlow<List<Note>> = noteRepo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val archivedNotes: StateFlow<List<Note>> = noteRepo.observeArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _search = MutableStateFlow("")
    private val _filter = MutableStateFlow(NoteFilter.ALL)

    val search: StateFlow<String> = _search
    val filter: StateFlow<NoteFilter> = _filter

    private val labelMap = noteRepo.observeNoteLabelJoins()
        .map { joins -> joins.groupBy({ it.noteId }, { it.label }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val backlinkCounts = noteRepo.observeBacklinkCounts()
        .map { rows -> rows.associate { it.id to it.cnt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val attachmentCounts = attachmentRepo.observeCountsByOwner(OwnerTypes.NOTE)
        .map { rows -> rows.associate { it.id to it.cnt } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val visibleNotes: StateFlow<List<NoteListItem>> =
        combine(
            combine(_search, _filter, allNotes, archivedNotes) { query, filter, active, archived ->
                val q = query.trim()
                val source = if (filter == NoteFilter.ARCHIVED) archived else active
                source
                    .filter { n -> filter != NoteFilter.PINNED || n.pinned }
                    .filter { n -> q.isEmpty() || n.title.contains(q, ignoreCase = true) || n.body.contains(q, ignoreCase = true) }
            },
            labelMap,
            backlinkCounts,
            attachmentCounts,
        ) { base, lm, bc, ac ->
            base.map { n ->
                NoteListItem(
                    note = n,
                    labels = lm[n.id] ?: emptyList(),
                    backlinkCount = bc[n.id] ?: 0,
                    attachmentCount = ac[n.id] ?: 0,
                )
            }
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearch(query: String) {
        _search.value = query
    }

    fun setFilter(filter: NoteFilter) {
        _filter.value = filter
    }

    fun togglePinned(note: Note) {
        viewModelScope.launch { noteRepo.setPinned(note, !note.pinned) }
    }

    fun archive(note: Note) {
        viewModelScope.launch { noteRepo.setArchived(note, true) }
    }

    fun restore(note: Note) {
        viewModelScope.launch { noteRepo.setArchived(note, false) }
    }

    fun delete(note: Note) {
        viewModelScope.launch { noteRepo.delete(note, onDeleteOwner = { _, _ -> }) }
    }

    suspend fun createDraft(): Long = noteRepo.create(title = "", body = "")
}