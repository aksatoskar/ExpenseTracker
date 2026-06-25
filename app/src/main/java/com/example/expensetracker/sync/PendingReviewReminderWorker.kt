package com.example.expensetracker.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.domain.crash.CrashReporter
import com.example.expensetracker.domain.usecase.report.SendPendingReviewReminderUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Runs daily at 9 PM and reminds the user about uncategorized pending transactions. */
@HiltWorker
class PendingReviewReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sendPendingReviewReminder: SendPendingReviewReminderUseCase,
    private val crashReporter: CrashReporter
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        sendPendingReviewReminder()
        Result.success()
    } catch (e: Exception) {
        crashReporter.recordNonFatal(e)
        Result.retry()
    }

    companion object {
        const val WORK_NAME = "daily-pending-review-reminder"
    }
}
