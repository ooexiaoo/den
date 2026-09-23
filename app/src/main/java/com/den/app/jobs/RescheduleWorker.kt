package com.den.app.jobs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.den.app.AppGraph

class RescheduleWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = AppGraph.container ?: return Result.success()
        return try {
            container.reminderScheduler.rescheduleAll()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}