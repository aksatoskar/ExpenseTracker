package com.example.expensetracker.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.data.TransactionType
import com.example.expensetracker.domain.ParsedTransaction
import java.util.concurrent.TimeUnit

class IngestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val parsed = inputData.toParsedTransaction() ?: return Result.failure()
        return try {
            (applicationContext as ExpenseTrackerApp).repository.ingest(parsed)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val KEY_AMOUNT_PAISE = "amount_paise"
        private const val KEY_MERCHANT = "merchant"
        private const val KEY_TYPE = "type"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_SOURCE = "source"
        private const val KEY_RAW_TEXT = "raw_text"

        fun enqueue(context: Context, parsed: ParsedTransaction) {
            val data = Data.Builder()
                .putLong(KEY_AMOUNT_PAISE, parsed.amountPaise)
                .putString(KEY_MERCHANT, parsed.merchant)
                .putString(KEY_TYPE, parsed.type.name)
                .putLong(KEY_TIMESTAMP, parsed.timestamp)
                .putString(KEY_SOURCE, parsed.source)
                .putString(KEY_RAW_TEXT, parsed.rawText)
                .build()

            val request = OneTimeWorkRequestBuilder<IngestWorker>()
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            val workName = "ingest-${parsed.timestamp}-${parsed.amountPaise}-${parsed.merchant.hashCode()}"
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        private fun Data.toParsedTransaction(): ParsedTransaction? {
            val amountPaise = getLong(KEY_AMOUNT_PAISE, -1L)
            val merchant = getString(KEY_MERCHANT) ?: return null
            val typeName = getString(KEY_TYPE) ?: return null
            val timestamp = getLong(KEY_TIMESTAMP, -1L)
            val source = getString(KEY_SOURCE) ?: return null
            val rawText = getString(KEY_RAW_TEXT) ?: return null
            if (amountPaise < 0 || timestamp < 0) return null
            val type = runCatching { TransactionType.valueOf(typeName) }.getOrNull() ?: return null
            return ParsedTransaction(amountPaise, merchant, type, timestamp, source, rawText)
        }
    }
}
