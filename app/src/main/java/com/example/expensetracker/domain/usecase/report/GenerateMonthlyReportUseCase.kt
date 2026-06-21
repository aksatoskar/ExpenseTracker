package com.example.expensetracker.domain.usecase.report

import com.example.expensetracker.core.money.formatInr
import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.data.local.entity.MonthlyReportEntity
import com.example.expensetracker.domain.model.Priority
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.BudgetRepository
import com.example.expensetracker.domain.repository.TransactionRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * On the 1st of the month, builds and persists the previous month's spending report and notifies
 * the user. No-op on other days or when there was no spend.
 */
class GenerateMonthlyReportUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val notifier: Notifier
) {
    suspend operator fun invoke() {
        if (LocalDate.now().dayOfMonth != 1) return
        val month = YearMonth.now().minusMonths(1)
        val transactions = transactionRepository.getDebitTransactions(DateRange.month(month))
        if (transactions.isEmpty()) return

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
        budgetRepository.upsertMonthlyReport(report)

        val name = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        notifier.showReport(
            "Your $name Spending Report is Ready",
            "Total spent: ${formatInr(total)}\nTop merchant: ${report.topMerchant}\nSavings estimate: ${formatInr(wasteful)}"
        )
    }
}
