package com.den.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.den.app.MainActivity
import com.den.app.R

class NotificationHelper(context: Context) {

    private val app = context.applicationContext
    private val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private companion object {
        private const val CHANNEL_REMINDERS = "reminders"
        private const val CHANNEL_BACKUPS = "backups"
        private const val BACKUP_NOTIFICATION_ID = 100_001
        private const val OPEN_REQUEST_SUFFIX = 3
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    app.getString(R.string.channel_reminders_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = app.getString(R.string.channel_reminders_desc) }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_BACKUPS,
                    app.getString(R.string.channel_backups_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = app.getString(R.string.channel_backups_desc) }
            )
        }
    }

    fun postTaskReminder(
        taskId: Long,
        title: String,
        dueText: String,
    ) {
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return
        val id = taskId.toInt()

        val openIntent = Intent(app, MainActivity::class.java).apply {
            action = "com.den.app.OPEN_TASK"
            putExtra("task_id", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPi = PendingIntent.getActivity(
            app, id * 10 + OPEN_REQUEST_SUFFIX, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(app, ReminderActionsReceiver::class.java).apply {
            action = ReminderActionsReceiver.ACTION_COMPLETE
            putExtra("task_id", taskId)
        }
        val completePi = PendingIntent.getBroadcast(
            app, id * 10 + 1, completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(app, ReminderActionsReceiver::class.java).apply {
            action = ReminderActionsReceiver.ACTION_SNOOZE
            putExtra("task_id", taskId)
            putExtra("task_title", title)
        }
        val snoozePi = PendingIntent.getBroadcast(
            app, id * 10 + 2, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(app, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title ?: "Task reminder")
            .setContentText(dueText.ifBlank { "Reminder" })
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .addAction(0, "Snooze 10 min", snoozePi)
            .addAction(0, "Done", completePi)
            .build()
        NotificationManagerCompat.from(app).notify(id, notification)
    }

    fun postBackupFailure(message: String) {
        val notification = NotificationCompat.Builder(app, CHANNEL_BACKUPS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Backup failed")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(app).notify(BACKUP_NOTIFICATION_ID, notification)
    }

    fun cancel(taskId: Long) {
        NotificationManagerCompat.from(app).cancel(taskId.toInt())
    }
}