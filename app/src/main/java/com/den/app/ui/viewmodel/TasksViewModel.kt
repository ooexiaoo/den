package com.den.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.den.app.AppContainer
import com.den.app.data.model.Task
import com.den.app.data.model.TaskWithSubtasks
import com.den.app.settings.Settings
import com.den.app.util.Dates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter { ALL, TODAY, UPCOMING, DONE }

data class TaskListItem(
    val task: Task,
    val done: Int,
    val total: Int,
)

class TasksViewModel(container: AppContainer) : ViewModel() {

    private val taskRepo = container.taskRepo

    val settings: StateFlow<Settings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    private val allWithSubtasks: StateFlow<List<TaskWithSubtasks>> = taskRepo.observeAllWithSubtasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    private val _search = MutableStateFlow("")

    val filter: StateFlow<TaskFilter> = _filter
    val search: StateFlow<String> = _search

    val visibleTasks: StateFlow<List<TaskListItem>> =
        combine(allWithSubtasks, _filter, _search) { rows, filter, query ->
            val q = query.trim()
            rows
                .asSequence()
                .filter { row ->
                    val task = row.task
                    when (filter) {
                        TaskFilter.ALL -> !task.completed
                        TaskFilter.TODAY -> {
                            !task.completed && (task.dueAt == null || Dates.startOfDay(task.dueAt) <= Dates.startOfDay(Dates.now()))
                        }
                        TaskFilter.UPCOMING -> {
                            !task.completed && task.dueAt != null && Dates.startOfDay(task.dueAt) > Dates.startOfDay(Dates.now())
                        }
                        TaskFilter.DONE -> task.completed
                    }
                }
                .filter { row ->
                    q.isEmpty() || row.task.title.contains(q, ignoreCase = true) || row.task.notes.contains(q, ignoreCase = true)
                }
                .sortedWith(
                    compareBy<TaskWithSubtasks> { it.task.completed }
                        .thenByDescending { it.task.pinned }
                        .thenBy { it.task.dueAt == null }
                        .thenBy { it.task.dueAt ?: Long.MAX_VALUE }
                        .thenByDescending { it.task.priority }
                        .thenByDescending { it.task.createdAt }
                )
                .map { TaskListItem(it.task, it.doneCount, it.totalCount) }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val openCount: StateFlow<Int> = combine(allWithSubtasks, _filter) { rows, filter ->
        when (filter) {
            TaskFilter.DONE -> rows.count { it.task.completed }
            else -> rows.count { !it.task.completed }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val allTasks: StateFlow<List<Task>> = allWithSubtasks
        .map { rows -> rows.map { it.task } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun setSearch(query: String) {
        _search.value = query
    }

    fun complete(task: Task, rating: Int?, reflection: String?) {
        viewModelScope.launch { taskRepo.toggleCompletion(task, rating, reflection) }
    }

    fun uncomplete(task: Task) {
        viewModelScope.launch { taskRepo.uncomplete(task) }
    }
}