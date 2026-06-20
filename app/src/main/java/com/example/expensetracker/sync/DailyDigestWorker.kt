package com.example.expensetracker.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.ExpenseDatabase
import com.example.expensetracker.data.TransactionStatus
import com.example.expensetracker.domain.formatInr
import com.example.expensetracker.notifications.ExpenseNotifications
import java.time.LocalDate
import java.time.ZoneId

class DailyDigestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val dao = ExpenseDatabase.get(applicationContext).expenseDao()
        val transactions = dao.getDebitTransactions(start, end)
        if (transactions.isEmpty()) return Result.success()

        val total = transactions.sumOf { it.amountPaise }
        val categoryLines = transactions
            .groupBy { it.category ?: Category.Other }
            .mapValues { entry -> entry.value.sumOf { it.amountPaise } }
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .joinToString("\n") { "${it.key.label}: ${formatInr(it.value)}" }
        val pending = transactions.count { it.status == TransactionStatus.PendingReview }

        ExpenseNotifications(applicationContext).showReport(
            "Today you spent ${formatInr(total)}",
            "$categoryLines\nPending reviews: $pending transactions"
        )
        return Result.success()
    }
}
