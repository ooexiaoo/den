package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.data.db.NoteTitleRow
import com.den.app.data.model.Attachment
import com.den.app.data.model.LabelWithCount
import com.den.app.data.model.Note
import com.den.app.data.model.OwnerTypes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val LINK_TOKEN = Regex("""\[\[(\d+):([^\]]*)\]\]""")

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditViewModel(
    container: AppContainer,
    initialNoteId: Long?,
) : ViewModel() {

    private val noteRepo = container.noteRepo

    private val _currentNoteId = MutableStateFlow(initialNoteId)
    val currentNoteId: StateFlow<Long?> = _currentNoteId

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body

    private val _loaded = MutableStateFlow(initialNoteId == null)
    val loaded: StateFlow<Boolean> = _loaded

    private val _pinned = MutableStateFlow(false)
    val pinned: StateFlow<Boolean> = _pinned

    private val _color = MutableStateFlow<Int?>(null)
    val color: StateFlow<Int?> = _color

    private val _mentionTargets = MutableStateFlow<List<NoteTitleRow>>(emptyList())
    val mentionTargets: StateFlow<List<NoteTitleRow>> = _mentionTargets

    private val _selectedLabels = MutableStateFlow<Set<Long>>(emptySet())
    val selectedLabels: StateFlow<Set<Long>> = _selectedLabels

    val allLabels: StateFlow<List<LabelWithCount>> =
        container.labelRepo.observeLabels()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var savedEntity: Note? = null
    private var saveJob: Job? = null

    val backlinks: StateFlow<List<Note>> =
        _currentNoteId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else noteRepo.observeBacklinks(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val outLinks: StateFlow<List<NoteTitleRow>> =
        combine(_body, _mentionTargets, _currentNoteId) { body, targets, selfId ->
            val byId = targets.associateBy { it.id }
            LINK_TOKEN.findAll(body)
                .map { it.groupValues[1].toLong() to it.groupValues[2] }
                .distinctBy { (id, _) -> id }
                .mapNotNull { (id, tokenTitle) ->
                    if (id == selfId) return@mapNotNull null
                    val target = byId[id] ?: return@mapNotNull null
                    NoteTitleRow(id, target.title.ifBlank { tokenTitle.ifBlank { "Untitled" } })
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val attachments: StateFlow<List<Attachment>> =
        _currentNoteId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else container.attachmentRepo.observeForOwner(OwnerTypes.NOTE, id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (initialNoteId != null) {
            viewModelScope.launch {
                val note = noteRepo.getNote(initialNoteId)
                if (note != null) {
                    savedEntity = note
                    _title.value = note.title
                    _body.value = note.body
                    _pinned.value = note.pinned
                    _color.value = note.colorIndex
                    _selectedLabels.value = container.labelRepo.observeNoteLabels(initialNoteId).first().map { it.id }.toSet()
                }
                _loaded.value = true
            }
        }
        refreshMentions()
    }

    fun onTitle(value: String) {
        _title.value = value
        scheduleSave()
    }

    fun onBody(value: String) {
        _body.value = value
        scheduleSave()
    }

    fun togglePinned() {
        viewModelScope.launch {
            _pinned.value = !_pinned.value
            saveNow()
        }
    }

    fun setColor(index: Int?) {
        viewModelScope.launch {
            _color.value = index
            saveNow()
        }
    }

    fun refreshMentions() {
        viewModelScope.launch {
            _mentionTargets.value = noteRepo.titleRows()
        }
    }

    fun insertMention(mentionId: Long, mentionTitle: String) {
        if (mentionId == _currentNoteId.value) return
        _body.value = insertMentionToken(_body.value, mentionId, mentionTitle)
    }

    fun toggleLabel(labelId: Long) {
        viewModelScope.launch {
            val current = _selectedLabels.value
            val next = if (labelId in current) current - labelId else current + labelId
            _selectedLabels.value = next
            val id = ensureSaved() ?: return@launch
            noteRepo.setLabels(id, next.sorted())
        }
    }

    suspend fun ensureSaved(): Long? {
        if (_currentNoteId.value == null) saveNow()
        return _currentNoteId.value
    }

    private fun insertMentionToken(current: String, id: Long, title: String): String {
        val token = "[[$id:$title]]"
        val trimmed = current.substringBeforeLast("[[")
        val base = if (trimmed.endsWith(" ")) trimmed else "$trimmed "
        return "$base$token"
    }

    fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(700)
            saveNow()
        }
    }

    suspend fun saveNow() {
        val titleValue = _title.value
        val bodyValue = _body.value
        val pinnedValue = _pinned.value
        val colorValue = _color.value

        val id = _currentNoteId.value
        if (id == null) {
            val created = noteRepo.create(title = titleValue, body = bodyValue, colorIndex = colorValue, pinned = pinnedValue)
            _currentNoteId.value = created
            savedEntity = Note(id = created, title = titleValue, body = bodyValue, colorIndex = colorValue, pinned = pinnedValue)
            return
        }
        val existing = savedEntity
        if (existing != null) {
            val updated = existing.copy(title = titleValue, body = bodyValue, pinned = pinnedValue, colorIndex = colorValue)
            noteRepo.update(updated)
            savedEntity = updated
        }
    }

    suspend fun delete() {
        val id = _currentNoteId.value ?: return
        val note = noteRepo.getNote(id) ?: return
        noteRepo.delete(note, onDeleteOwner = { _, _ -> })
    }
}