package com.example.expensetracker.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.domain.usecase.report.GenerateMonthlyReportUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Generates and notifies the previous month's report on the 1st of each month. */
@HiltWorker
class MonthlyReportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val generateMonthlyReport: GenerateMonthlyReportUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        generateMonthlyReport()
        return Result.success()
    }
}
