package com.example.expensetracker.domain.usecase.budget

import com.example.expensetracker.domain.repository.BudgetRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import javax.inject.Inject

/**
 * Auto-renews budgets monthly: archives every elapsed month since the last archive point and,
 * if anything was archived, resets all alert flags so the new month starts clean. Idempotent —
 * safe to call on every app open.
 */
class RenewBudgetsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val archiveMonth: ArchiveMonthUseCase
) {
    suspend operator fun invoke() {
        val current = YearMonth.now()
        val stored = settingsRepository.lastBudgetArchiveMonth.first()
        val lastArchived = stored?.let { runCatching { YearMonth.parse(it) }.getOrNull() }

        if (lastArchived == null) {
            // First run: establish a baseline without retroactively archiving.
            settingsRepository.setLastBudgetArchiveMonth(current.minusMonths(1).toString())
            return
        }

        var cursor = lastArchived.plusMonths(1)
        var archivedAny = false
        while (cursor.isBefore(current)) {
            archiveMonth(cursor)
            archivedAny = true
            cursor = cursor.plusMonths(1)
        }
        settingsRepository.setLastBudgetArchiveMonth(current.minusMonths(1).toString())
        if (archivedAny) resetAllBudgetAlertFlags()
    }

    private suspend fun resetAllBudgetAlertFlags() {
        budgetRepository.getBudgets().forEach { budget ->
            if (budget.alert50Sent || budget.alert75Sent || budget.alert90Sent || budget.alert100Sent) {
                budgetRepository.updateBudget(
                    budget.copy(alert50Sent = false, alert75Sent = false, alert90Sent = false, alert100Sent = false)
                )
            }
        }
    }
}
