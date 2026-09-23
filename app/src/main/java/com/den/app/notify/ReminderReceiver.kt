package com.den.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.den.app.util.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val taskId = intent.getLongExtra("task_id", -1L)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val pending = goAsync()
        scope.launch {
            try {
                if (taskId >= 0) {
                    val title = intent.getStringExtra("task_title") ?: "Task reminder"
                    val dueAt = intent.getLongExtra("task_due", taskId)
                    val helper = NotificationHelper(app)
                    helper.ensureChannels()
                    val dueText = if (intent.hasExtra("task_due")) "Due ${Dates.humanDue(dueAt)}" else "Reminder"
                    helper.postTaskReminder(taskId, title, dueText)
                }
                ReminderScheduler(app).rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}