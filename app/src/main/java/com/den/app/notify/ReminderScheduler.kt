package com.den.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.den.app.data.model.Task
import com.den.app.util.Dates

class ReminderScheduler(context: Context) {

    private val app = context.applicationContext
    private val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms()
        else true

    fun schedule(task: Task) {
        val triggerAt = task.reminderAt ?: return
        scheduleAt(task.id, task.title, task.dueAt, triggerAt)
    }

    fun scheduleSnooze(taskId: Long, title: String, dueAt: Long?) {
        scheduleAt(taskId, title, dueAt, System.currentTimeMillis() + 10 * 60 * 1000L)
    }

    private fun scheduleAt(taskId: Long, title: String, dueAt: Long?, triggerAt: Long) {
        cancel(taskId)
        val intent = Intent(app, ReminderReceiver::class.java).apply {
            putExtra("task_id", taskId)
            putExtra("task_title", title)
            putExtra("task_due", dueAt ?: triggerAt)
        }
        val pi = PendingIntent.getBroadcast(
            app, taskId.toInt() * 10, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(taskId: Long) {
        val intent = Intent(app, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            app, taskId.toInt() * 10, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pi?.let { alarmManager.cancel(it) }
    }

    fun dueText(dueAt: Long?): String = if (dueAt != null) {
        "Due ${Dates.humanDue(dueAt)}"
    } else {
        "You have a task waiting"
    }
}