package com.example.expensetracker.data

import android.content.Context
import com.example.expensetracker.domain.ParsedTransaction
import com.example.expensetracker.domain.TransactionParser
import com.example.expensetracker.domain.normalizeMerchant
import com.example.expensetracker.notifications.ExpenseNotifications
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ExpenseRepository(
    private val context: Context,
    private val dao: ExpenseDao
) {
    val latestTransactions: Flow<List<TransactionEntity>> = dao.observeLatestTransactions()
    val allTransactions: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    val pendingTransactions: Flow<List<TransactionEntity>> = dao.observePendingTransactions()
    val pendingCount: Flow<Int> = dao.observePendingCount()
    val budgets: Flow<List<BudgetEntity>> = dao.observeBudgets()
    val reports: Flow<List<MonthlyReportEntity>> = dao.observeReports()
    val budgetHistory: Flow<List<BudgetHistoryEntity>> = dao.observeBudgetHistory()

    suspend fun ingest(parsed: ParsedTransaction): Boolean {
        val key = normalizeMerchant(parsed.merchant)
        val duplicate = dao.findRecentAmountMatches(
            amountPaise = parsed.amountPaise,
            type = parsed.type,
            start = parsed.timestamp - DUPLICATE_WINDOW_MILLIS,
            end = parsed.timestamp + DUPLICATE_WINDOW_MILLIS
        ).firstOrNull { normalizeMerchant(it.merchant) == key }

        if (duplicate != null) {
            ensureNotificationShown(duplicate)
            return false
        }

        val rule = dao.getMerchantRule(key)
        val transaction = TransactionEntity(
            amountPaise = parsed.amountPaise,
            merchant = parsed.merchant,
            type = parsed.type,
            timestamp = parsed.timestamp,
            source = parsed.source,
            rawText = parsed.rawText,
            category = rule?.category,
            priority = rule?.priority,
            status = TransactionStatus.PendingReview,
            notified = false
        )
        val id = dao.insertTransaction(transaction)
        ensureNotificationShown(transaction.copy(id = id))
        transaction.category?.let { checkBudgetAlerts(it) }
        return true
    }

    suspend fun checkBudgetAlerts(category: Category) {
        val budget = dao.getBudget(category) ?: return
        val range = DateRange.month()
        val spent = dao.getCategorySpent(category, range.startMillis, range.endMillis)
        val limit = budget.limitPaise.coerceAtLeast(1)
        val percent = spent * 100f / limit

        if (percent < 50f) {
            val anySent = budget.alert50Sent || budget.alert75Sent || budget.alert90Sent || budget.alert100Sent
            if (anySent) {
                dao.updateBudget(
                    budget.copy(alert50Sent = false, alert75Sent = false, alert90Sent = false, alert100Sent = false)
                )
            }
            return
        }

        val crossed50 = percent >= 50f
        val crossed75 = percent >= 75f
        val crossed90 = percent >= 90f
        val crossed100 = percent >= 100f

        val newThreshold = when {
            crossed100 && !budget.alert100Sent -> 100
            crossed90 && !budget.alert90Sent -> 90
            crossed75 && !budget.alert75Sent -> 75
            crossed50 && !budget.alert50Sent -> 50
            else -> null
        }

        val updated = budget.copy(
            alert50Sent = budget.alert50Sent || crossed50,
            alert75Sent = budget.alert75Sent || crossed75,
            alert90Sent = budget.alert90Sent || crossed90,
            alert100Sent = budget.alert100Sent || crossed100
        )
        if (updated != budget) dao.updateBudget(updated)

        if (newThreshold != null) {
            ExpenseNotifications(context).showBudgetAlert(category, newThreshold, spent, budget.limitPaise)
        }
    }

    suspend fun syncSmsInbox(sinceMillis: Long): Int {
        val parser = TransactionParser()
        var newCount = 0
        SmsReader(context).readSince(sinceMillis).forEach { sms ->
            val parsed = parser.parse(sms.body, "SMS", sms.timestamp) ?: return@forEach
            if (ingest(parsed)) newCount++
        }
        return newCount
    }

    suspend fun notifyPendingUnnotified() {
        dao.getPendingTransactions()
            .filter { !it.notified }
            .forEach { ensureNotificationShown(it) }
    }

    private suspend fun ensureNotificationShown(transaction: TransactionEntity) {
        if (transaction.notified || transaction.status != TransactionStatus.PendingReview) return
        val shown = ExpenseNotifications(context).showDetected(transaction.id, transaction)
        if (shown) {
            dao.updateTransaction(transaction.copy(notified = true))
        }
    }

    suspend fun saveReview(
        transaction: TransactionEntity,
        category: Category,
        priority: Priority,
        notes: String
    ) {
        val reviewed = transaction.copy(
            category = category,
            priority = priority,
            notes = notes,
            status = TransactionStatus.Reviewed
        )
        dao.updateTransaction(reviewed)
        dao.upsertMerchantRule(
            MerchantRuleEntity(
                merchantKey = normalizeMerchant(transaction.merchant),
                displayMerchant = transaction.merchant,
                category = category,
                priority = priority,
                updatedAt = System.currentTimeMillis()
            )
        )
        checkBudgetAlerts(category)
    }

    suspend fun skip(transaction: TransactionEntity) {
        dao.updateTransaction(transaction.copy(status = TransactionStatus.Skipped))
    }

    suspend fun update(transaction: TransactionEntity) = dao.updateTransaction(transaction)

    suspend fun delete(id: Long) {
        val existing = dao.getTransaction(id)
        dao.deleteTransaction(id)
        ExpenseNotifications(context).cancel(id)
        existing?.category?.let { checkBudgetAlerts(it) }
    }

    suspend fun getTransaction(id: Long) = dao.getTransaction(id)
    fun observeTransaction(id: Long) = dao.observeTransaction(id)

    fun observeTotal(range: DateRange) = dao.observeDebitTotal(range.startMillis, range.endMillis)
    fun observeCategoryTotals(range: DateRange) = dao.observeCategoryTotals(range.startMillis, range.endMillis)
    fun observePriorityTotals(range: DateRange) = dao.observePriorityTotals(range.startMillis, range.endMillis)
    fun observeTopMerchants(range: DateRange, limit: Int = 5) = dao.observeTopMerchants(range.startMillis, range.endMillis, limit)

    suspend fun upsertBudget(category: Category, limitPaise: Long, existing: BudgetEntity? = null) {
        dao.upsertBudget((existing ?: BudgetEntity(category = category, limitPaise = limitPaise)).copy(limitPaise = limitPaise))
        checkBudgetAlerts(category)
    }

    suspend fun deleteBudget(id: Long) = dao.deleteBudget(id)

    suspend fun archiveMonth(yearMonth: YearMonth) {
        val activeBudgets = dao.getBudgets()
        if (activeBudgets.isEmpty()) return
        val range = DateRange.month(yearMonth)
        val label = yearMonth.toString()
        activeBudgets.forEach { budget ->
            val spent = dao.getCategorySpent(budget.category, range.startMillis, range.endMillis)
            dao.upsertBudgetHistory(
                BudgetHistoryEntity(
                    yearMonth = label,
                    category = budget.category,
                    limitPaise = budget.limitPaise,
                    spentPaise = spent,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun resetAllBudgetAlertFlags() {
        dao.getBudgets().forEach { budget ->
            if (budget.alert50Sent || budget.alert75Sent || budget.alert90Sent || budget.alert100Sent) {
                dao.updateBudget(
                    budget.copy(alert50Sent = false, alert75Sent = false, alert90Sent = false, alert100Sent = false)
                )
            }
        }
    }

    suspend fun generatePreviousMonthReport() {
        val previous = YearMonth.now().minusMonths(1)
        val range = DateRange.month(previous)
        val transactions = dao.observeAllTransactions()
        @Suppress("UNUSED_VARIABLE") val unused = transactions
        dao.upsertMonthlyReport(
            MonthlyReportEntity(
                yearMonth = previous.toString(),
                totalPaise = 0,
                topMerchant = "Calculated on open",
                essentialPaise = 0,
                optionalPaise = 0,
                wastefulPaise = 0,
                savingsEstimatePaise = 0,
                generatedAt = System.currentTimeMillis()
            )
        )
    }
}

private const val DUPLICATE_WINDOW_MILLIS = 10 * 60 * 1000L

data class DateRange(val startMillis: Long, val endMillis: Long) {
    companion object {
        private val zone = ZoneId.systemDefault()

        fun today(): DateRange {
            val start = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
            val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return DateRange(start, end)
        }

        fun week(): DateRange {
            val now = LocalDate.now()
            val startDate = now.minusDays((now.dayOfWeek.value - 1).toLong())
            val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = startDate.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return DateRange(start, end)
        }

        fun lastWeek(): DateRange {
            val now = LocalDate.now()
            val startThisWeek = now.minusDays((now.dayOfWeek.value - 1).toLong())
            val startLastWeek = startThisWeek.minusDays(7)
            val start = startLastWeek.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = startThisWeek.atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return DateRange(start, end)
        }

        fun month(month: YearMonth = YearMonth.now()): DateRange {
            val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            return DateRange(start, end)
        }
    }
}
