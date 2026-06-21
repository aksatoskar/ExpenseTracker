package com.example.expensetracker.domain.usecase.budget

import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.BudgetRepository
import com.example.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Evaluates a category's month-to-date spend against its budget and fires a single alert when a
 * new 50/75/90/100% threshold is crossed. Sent-flags on the budget prevent duplicate alerts and
 * are reset once spending drops back below 50% (e.g. after a deletion).
 */
class CheckBudgetAlertsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val notifier: Notifier
) {
    suspend operator fun invoke(category: Category) {
        val budget = budgetRepository.getBudget(category) ?: return
        val range = DateRange.month()
        val spent = transactionRepository.getCategorySpent(category, range)
        val limit = budget.limitPaise.coerceAtLeast(1)
        val percent = spent * 100f / limit

        if (percent < 50f) {
            val anySent = budget.alert50Sent || budget.alert75Sent || budget.alert90Sent || budget.alert100Sent
            if (anySent) {
                budgetRepository.updateBudget(
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
        if (updated != budget) budgetRepository.updateBudget(updated)

        if (newThreshold != null) {
            notifier.showBudgetAlert(category, newThreshold, spent, budget.limitPaise)
        }
    }
}
