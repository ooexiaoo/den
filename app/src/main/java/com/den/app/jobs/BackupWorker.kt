package com.den.app.jobs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.den.app.AppGraph

class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = AppGraph.container ?: return Result.success()
        val backup = container.backupManager
        val cfg = backup.currentSettings()
        if (!cfg.backupEnabled) return Result.success()
        if (!backup.isDue(cfg)) return Result.success()
        val result = backup.createAutoBackup(force = true)
        return if (result.isSuccess) {
            Result.success()
        } else {
            val message = result.exceptionOrNull()?.message ?: "Unknown error"
            container.notificationHelper.postBackupFailure(message)
            Result.retry()
        }
    }
}