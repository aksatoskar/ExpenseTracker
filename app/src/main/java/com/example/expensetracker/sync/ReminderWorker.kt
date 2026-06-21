package com.example.expensetracker.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.domain.usecase.report.SendReviewReminderUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Posts a deferred "review this expense" reminder for a still-pending transaction. */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sendReviewReminder: SendReviewReminderUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_TRANSACTION_ID, -1L)
        if (id <= 0) return Result.success()
        sendReviewReminder(id)
        return Result.success()
    }

    companion object {
        const val KEY_TRANSACTION_ID = "transaction_id"
    }
}
