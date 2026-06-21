package com.example.expensetracker.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.expensetracker.domain.usecase.transaction.NotifyPendingUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Re-notifies any pending transactions that were never surfaced (missed-notification recovery). */
@HiltWorker
class PendingNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notifyPending: NotifyPendingUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            notifyPending()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "pending-expense-notifications"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<PendingNotificationWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
