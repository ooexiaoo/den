package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.data.model.TaskWithSubtasks
import com.den.app.util.Dates
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ReviewStats(
    val overdue: List<TaskWithSubtasks>,
    val dueToday: List<TaskWithSubtasks>,
    val upNext: List<TaskWithSubtasks>,
    val noDate: List<TaskWithSubtasks>,
    val completedToday: List<TaskWithSubtasks>,
    val noteCount: Int,
)

class ReviewViewModel(container: AppContainer) : ViewModel() {

    private val tasks: StateFlow<List<TaskWithSubtasks>> = container.taskRepo.observeAllWithSubtasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val notes = container.noteRepo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<ReviewStats> = combine(tasks, notes) { rows, noteList ->
        val now = Dates.now()
        val dayStart = Dates.startOfDay(now)
        val dayEnd = Dates.endOfDay(now)
        val weekEnd = dayStart + 7L * 86_400_000L

        val overdue = mutableListOf<TaskWithSubtasks>()
        val dueToday = mutableListOf<TaskWithSubtasks>()
        val upNext = mutableListOf<TaskWithSubtasks>()
        val noDate = mutableListOf<TaskWithSubtasks>()
        val completedToday = mutableListOf<TaskWithSubtasks>()

        rows.forEach { row ->
            val t = row.task
            when {
                t.completed -> {
                    val at = t.completedAt ?: return@forEach
                    if (at in dayStart..dayEnd) completedToday.add(row)
                }
                t.dueAt == null -> noDate.add(row)
                t.dueAt < dayStart -> overdue.add(row)
                t.dueAt <= dayEnd -> dueToday.add(row)
                t.dueAt <= weekEnd -> upNext.add(row)
            }
        }

        ReviewStats(
            overdue = overdue,
            dueToday = dueToday,
            upNext = upNext,
            noDate = noDate,
            completedToday = completedToday,
            noteCount = noteList.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewStats(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), 0))
}