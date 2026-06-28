package com.example.expensetracker.domain.usecase.budget

import com.example.expensetracker.domain.repository.BudgetRepository
import javax.inject.Inject

/** Removes a budget and records a sync tombstone so it is not restored from the cloud. */
class DeleteBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(id: Long) {
        val budget = budgetRepository.getBudgetById(id) ?: return
        budgetRepository.markBudgetDeleted(budget.category)
        budgetRepository.deleteBudget(id)
    }
}
