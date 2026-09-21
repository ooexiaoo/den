package com.den.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.den.app.util.Dates

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1L)
        if (taskId < 0) return
        val title = intent.getStringExtra("task_title") ?: "Task reminder"
        val dueAt = intent.getLongExtra("task_due", taskId)

        val app = context.applicationContext
        val helper = NotificationHelper(app)
        helper.ensureChannels()
        val dueText = if (intent.hasExtra("task_due")) "Due ${Dates.humanDue(dueAt)}" else "Reminder"
        helper.postTaskReminder(taskId, title, dueText)
    }
}