package com.den.app

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.den.app.backup.BackupManager
import com.den.app.crypto.AppCrypto
import com.den.app.data.db.DenDatabase
import com.den.app.data.repo.AttachmentRepository
import com.den.app.data.repo.LabelRepository
import com.den.app.data.repo.NoteRepository
import com.den.app.data.repo.TaskRepository
import com.den.app.jobs.BackupWorker
import com.den.app.jobs.RescheduleWorker
import com.den.app.media.MediaImporter
import com.den.app.notify.NotificationHelper
import com.den.app.notify.ReminderScheduler
import com.den.app.port.PortManager
import com.den.app.settings.PasscodeManager
import com.den.app.settings.SettingsStore
import com.den.app.ui.viewmodel.DenViewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.TimeUnit

sealed class NavRequest {
    data class OpenTask(val id: Long) : NavRequest()
    data class OpenNote(val id: Long) : NavRequest()
}

object AppGraph {
    @Volatile
    var container: AppContainer? = null
}

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val crypto = AppCrypto(appContext)
    val settings = SettingsStore(appContext)
    val notificationHelper = NotificationHelper(appContext)
    val reminderScheduler = ReminderScheduler(appContext)
    val database: DenDatabase = DenDatabase.build(appContext, crypto.dbPassphrase())

    val attachmentRepo = AttachmentRepository(appContext, database.attachmentDao())
    val taskRepo = TaskRepository(
        database.taskDao(),
        database.subtaskDao(),
        database.labelDao(),
        reminderScheduler,
    )
    val noteRepo = NoteRepository(database.noteDao(), database.labelDao())
    val labelRepo = LabelRepository(database.labelDao())
    val backupManager = BackupManager(appContext, database, attachmentRepo, settings, crypto)
    val mediaImporter = MediaImporter(appContext, attachmentRepo)
    val portManager = PortManager(appContext, database)
    val passcode = PasscodeManager(settings, crypto)

    private val navChannel = Channel<NavRequest>(Channel.BUFFERED)
    val navRequests: Flow<NavRequest> = navChannel.receiveAsFlow()

    lateinit var vmFactory: DenViewModelFactory

    init {
        vmFactory = DenViewModelFactory(this)
        notificationHelper.ensureChannels()

        val workManager = WorkManager.getInstance(appContext)
        workManager.enqueueUniqueWork(
            "reschedule_reminders",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RescheduleWorker>().build(),
        )
        workManager.enqueueUniquePeriodicWork(
            "auto_backup",
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<BackupWorker>(15, TimeUnit.MINUTES).build(),
        )
    }

    fun requestNav(request: NavRequest) {
        navChannel.trySend(request)
    }
}