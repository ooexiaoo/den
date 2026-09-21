package com.den.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.den.app.AppGraph
import com.den.app.data.model.OwnerTypes
import com.den.app.util.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderActionsReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.den.app.action.COMPLETE"
        const val ACTION_SNOOZE = "com.den.app.action.SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1L)
        if (taskId < 0) return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_COMPLETE -> complete(context, taskId)
                    ACTION_SNOOZE -> snooze(context, intent, taskId)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun complete(context: Context, taskId: Long) {
        val container = AppGraph.container
        val task = container.taskRepo.getTask(taskId)
        val helper = NotificationHelper(context)
        if (task == null) {
            helper.cancel(taskId)
            return
        }
        if (!task.completed) {
            container.taskRepo.toggleCompletion(task, null, null)
        }
        helper.cancel(taskId)
    }

    private suspend fun snooze(context: Context, intent: Intent, taskId: Long) {
        val title = intent.getStringExtra("task_title") ?: "Task reminder"
        val dueAt = intent.getLongExtra("task_due", 0L).takeIf { it > 0 }

        val app = context.applicationContext
        val scheduler = ReminderScheduler(app)
        val helper = NotificationHelper(app)
        helper.ensureChannels()
        scheduler.scheduleSnooze(taskId, title, dueAt)
        val next = System.currentTimeMillis() + 10 * 60 * 1000L
        helper.postTaskReminder(
            taskId,
            title,
            "Snoozed · ${Dates.formatTime(next)}"
        )
    }
}