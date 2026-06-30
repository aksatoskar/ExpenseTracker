package com.example.expensetracker.domain.usecase.budget

import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.repository.BudgetRepository
import com.example.expensetracker.domain.sync.CloudSyncScheduler
import javax.inject.Inject

/** Creates or updates a category's monthly limit, then re-evaluates its alert thresholds. */
class UpsertBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val checkBudgetAlerts: CheckBudgetAlertsUseCase,
    private val cloudSyncScheduler: CloudSyncScheduler
) {
    suspend operator fun invoke(category: Category, limitPaise: Long, existing: BudgetEntity? = null) {
        budgetRepository.clearBudgetDeleted(category)
        budgetRepository.upsertBudget(
            (existing ?: BudgetEntity(category = category, limitPaise = limitPaise)).copy(limitPaise = limitPaise)
        )
        checkBudgetAlerts(category)
        cloudSyncScheduler.schedule()
    }
}
