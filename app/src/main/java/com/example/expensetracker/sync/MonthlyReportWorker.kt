package com.example.expensetracker.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.data.ExpenseDatabase
import com.example.expensetracker.data.MonthlyReportEntity
import com.example.expensetracker.data.Priority
import com.example.expensetracker.domain.formatInr
import com.example.expensetracker.notifications.ExpenseNotifications
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class MonthlyReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (LocalDate.now().dayOfMonth != 1) return Result.success()
        val month = YearMonth.now().minusMonths(1)
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val dao = ExpenseDatabase.get(applicationContext).expenseDao()
        val transactions = dao.getDebitTransactions(start, end)
        if (transactions.isEmpty()) return Result.success()

        val byMerchant = transactions.groupBy { it.merchant }.mapValues { it.value.sumOf { tx -> tx.amountPaise } }
        val essential = transactions.filter { it.priority == Priority.Essential }.sumOf { it.amountPaise }
        val optional = transactions.filter { it.priority == Priority.Optional }.sumOf { it.amountPaise }
        val wasteful = transactions.filter { it.priority == Priority.Wasteful }.sumOf { it.amountPaise }
        val total = transactions.sumOf { it.amountPaise }
        val report = MonthlyReportEntity(
            yearMonth = month.toString(),
            totalPaise = total,
            topMerchant = byMerchant.maxByOrNull { it.value }?.key ?: "Unknown",
            essentialPaise = essential,
            optionalPaise = optional,
            wastefulPaise = wasteful,
            savingsEstimatePaise = wasteful,
            generatedAt = System.currentTimeMillis()
        )
        dao.upsertMonthlyReport(report)

        val name = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        ExpenseNotifications(applicationContext).showReport(
            "Your $name Spending Report is Ready",
            "Total spent: ${formatInr(total)}\nTop merchant: ${report.topMerchant}\nSavings estimate: ${formatInr(wasteful)}"
        )
        return Result.success()
    }
}
