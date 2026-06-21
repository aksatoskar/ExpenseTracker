package com.example.expensetracker.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.domain.usecase.report.SendDailyDigestUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Posts the end-of-day spending digest. */
@HiltWorker
class DailyDigestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sendDailyDigest: SendDailyDigestUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        sendDailyDigest()
        return Result.success()
    }
}
