package com.example.expensetracker.domain.usecase.transaction

import com.example.expensetracker.core.money.normalizeMerchant
import com.example.expensetracker.data.local.entity.MerchantRuleEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Priority
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.usecase.budget.CheckBudgetAlertsUseCase
import javax.inject.Inject

/**
 * Confirms a transaction with the user's chosen amount/category/priority, marks it `Reviewed`,
 * remembers the merchant rule for future auto-categorization and re-checks the budget.
 */
class SaveReviewUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val checkBudgetAlerts: CheckBudgetAlertsUseCase
) {
    suspend operator fun invoke(
        transaction: TransactionEntity,
        amountPaise: Long,
        category: Category,
        priority: Priority,
        notes: String
    ) {
        transactionRepository.update(
            transaction.copy(
                amountPaise = amountPaise,
                category = category,
                priority = priority,
                notes = notes,
                status = TransactionStatus.Reviewed
            )
        )
        transactionRepository.upsertMerchantRule(
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
}
