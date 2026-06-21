package com.example.expensetracker.domain.usecase.budget

import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.data.local.entity.BudgetHistoryEntity
import com.example.expensetracker.domain.repository.BudgetRepository
import com.example.expensetracker.domain.repository.TransactionRepository
import java.time.YearMonth
import javax.inject.Inject

/** Snapshots each active budget's limit vs. actual spend for the given month into history. */
class ArchiveMonthUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(yearMonth: YearMonth) {
        val activeBudgets = budgetRepository.getBudgets()
        if (activeBudgets.isEmpty()) return
        val range = DateRange.month(yearMonth)
        val label = yearMonth.toString()
        activeBudgets.forEach { budget ->
            val spent = transactionRepository.getCategorySpent(budget.category, range)
            budgetRepository.upsertBudgetHistory(
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
}
