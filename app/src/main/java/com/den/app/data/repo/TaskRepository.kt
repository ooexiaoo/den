package com.den.app.data.repo

import com.den.app.data.db.LabelDao
import com.den.app.data.db.SubtaskDao
import com.den.app.data.db.TaskDao
import com.den.app.data.model.OwnerTypes
import com.den.app.data.model.Subtask
import com.den.app.data.model.Task
import com.den.app.data.model.TaskLabelCrossRef
import com.den.app.data.model.TaskWithSubtasks
import com.den.app.notify.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val labelDao: LabelDao,
    private val reminderScheduler: ReminderScheduler,
) {
    fun observeActive(): Flow<List<Task>> = taskDao.observeActive()

    fun observeAllWithSubtasks(): Flow<List<TaskWithSubtasks>> = taskDao.observeAllWithSubtasks()

    fun observeArchivedWithSubtasks(): Flow<List<TaskWithSubtasks>> = taskDao.observeArchivedWithSubtasks()

    fun observeById(id: Long): Flow<Task?> = taskDao.observeById(id)

    fun observeWithSubtasks(id: Long): Flow<TaskWithSubtasks?> = taskDao.observeTaskWithSubtasks(id)

    fun observeSubtasks(taskId: Long): Flow<List<Subtask>> = subtaskDao.observeForTask(taskId)

    suspend fun taskWithSubtasks(id: Long): TaskWithSubtasks? = taskDao.getTaskWithSubtasks(id)

    suspend fun getTask(id: Long): Task? = taskDao.getById(id)

    suspend fun pendingReminderTasks(): List<Task> = taskDao.tasksWithReminders()

    suspend fun allTasks(): List<Task> = taskDao.allForBackup()

    suspend fun create(
        title: String,
        notes: String = "",
        dueAt: Long? = null,
        reminderAt: Long? = null,
        priority: Int = 0,
        colorIndex: Int? = null,
        pinned: Boolean = false,
    ): Long {
        val now = System.currentTimeMillis()
        val id = taskDao.insert(
            Task(
                title = title,
                notes = notes,
                dueAt = dueAt,
                reminderAt = reminderAt,
                priority = priority,
                colorIndex = colorIndex,
                pinned = pinned,
                createdAt = now,
                updatedAt = now,
            )
        )
        recheckReminder(id)
        return id
    }

    suspend fun update(task: Task) {
        taskDao.update(task.copy(updatedAt = System.currentTimeMillis()))
        recheckReminder(task.id)
    }

    suspend fun setLabels(taskId: Long, labelIds: List<Long>) {
        labelDao.deleteTaskCrossRefsForTask(taskId)
        if (labelIds.isNotEmpty()) {
            labelDao.insertTaskCrossRefs(labelIds.map { TaskLabelCrossRef(taskId, it) })
        }
    }

    suspend fun addSubtask(taskId: Long, title: String, done: Boolean = false) {
        val count = subtaskDao.listForTask(taskId).size
        subtaskDao.insert(Subtask(taskId = taskId, title = title, done = done, sortOrder = count))
    }

    suspend fun updateSubtask(subtask: Subtask) {
        subtaskDao.update(subtask)
    }

    suspend fun setSubtaskDone(subtask: Subtask, done: Boolean) {
        subtaskDao.setDone(subtask.id, done)
    }

    suspend fun deleteSubtask(subtask: Subtask) {
        subtaskDao.delete(subtask)
    }

    suspend fun toggleCompletion(task: Task, rating: Int?, reflection: String?) {
        if (!task.completed) {
            val now = System.currentTimeMillis()
            taskDao.setCompletion(task.id, true, rating, reflection, now, now)
            reminderScheduler.cancel(task.id)
        } else {
            taskDao.setCompletion(task.id, false, null, null, null, System.currentTimeMillis())
            recheckReminder(task.id)
        }
    }

    suspend fun uncomplete(task: Task) = toggleCompletion(task, null, null)

    suspend fun setPinned(task: Task, pinned: Boolean) {
        taskDao.setPinned(task.id, pinned, System.currentTimeMillis())
    }

    suspend fun setArchived(task: Task, archived: Boolean) {
        taskDao.setArchived(task.id, archived, System.currentTimeMillis())
    }

    suspend fun duplicate(task: Task): Long {
        val now = System.currentTimeMillis()
        val newId = taskDao.insert(
            task.copy(
                id = 0,
                completed = false,
                completeRating = null,
                completeReflection = null,
                completedAt = null,
                archived = false,
                pinned = false,
                createdAt = now,
                updatedAt = now,
                sortOrder = 0L,
                reminderAt = null,
            )
        )
        subtaskDao.listForTask(task.id).forEach { sub ->
            subtaskDao.insert(sub.copy(id = 0, taskId = newId))
        }
        labelDao.labelsForTask(task.id).forEach { label ->
            labelDao.insertTaskCrossRefs(listOf(TaskLabelCrossRef(newId, label.id)))
        }
        return newId
    }

    suspend fun delete(task: Task, onDeleteOwner: suspend (String, Long) -> Unit) {
        reminderScheduler.cancel(task.id)
        onDeleteOwner(OwnerTypes.TASK, task.id)
        taskDao.delete(task)
    }

    suspend fun moveSubtask(from: Subtask, toIndex: Int) {
        val all = subtaskDao.listForTask(from.taskId).sortedBy { it.sortOrder }
        val current = all.indexOfFirst { it.id == from.id }
        if (current < 0) return
        val reordered = all.toMutableList()
        val item = reordered.removeAt(current)
        reordered.add(toIndex.coerceIn(0, reordered.size), item)
        reordered.forEachIndexed { index, sub ->
            subtaskDao.update(sub.copy(sortOrder = index))
        }
    }

    private suspend fun recheckReminder(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        if (task.reminderAt != null && !task.completed) {
            reminderScheduler.schedule(task)
        } else {
            reminderScheduler.cancel(taskId)
        }
    }
}