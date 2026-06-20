package com.example.expensetracker.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.data.ExpenseDatabase
import com.example.expensetracker.data.TransactionStatus
import com.example.expensetracker.domain.formatInr
import com.example.expensetracker.notifications.ExpenseNotifications

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_TRANSACTION_ID, -1L)
        val transaction = ExpenseDatabase.get(applicationContext).expenseDao().getTransaction(id)
            ?: return Result.success()
        if (transaction.status != TransactionStatus.PendingReview) return Result.success()
        ExpenseNotifications(applicationContext).showReminder(
            id,
            "Review expense",
            "${formatInr(transaction.amountPaise)} spent at ${transaction.merchant}"
        )
        return Result.success()
    }

    companion object {
        const val KEY_TRANSACTION_ID = "transaction_id"
    }
}
