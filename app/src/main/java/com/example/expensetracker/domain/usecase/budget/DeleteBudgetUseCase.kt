package com.example.expensetracker.domain.usecase.budget

import com.example.expensetracker.domain.repository.BudgetRepository
import javax.inject.Inject

/** Removes a budget by id. */
class DeleteBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(id: Long) = budgetRepository.deleteBudget(id)
}
