package com.example.expensetracker.domain.usecase.transaction

import com.example.expensetracker.core.money.normalizeMerchant
import com.example.expensetracker.data.local.entity.MerchantRuleEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.CategorySelection
import com.example.expensetracker.domain.model.Priority
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.model.toEntityFields
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.sync.CloudSyncScheduler
import com.example.expensetracker.domain.usecase.budget.CheckBudgetAlertsUseCase
import javax.inject.Inject

/**
 * Confirms a transaction with the user's chosen amount/category/priority, marks it `Reviewed`,
 * remembers the merchant rule for future auto-categorization and re-checks the budget.
 */
class SaveReviewUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val checkBudgetAlerts: CheckBudgetAlertsUseCase,
    private val cloudSyncScheduler: CloudSyncScheduler
) {
    suspend operator fun invoke(
        transaction: TransactionEntity,
        amountPaise: Long,
        categorySelection: CategorySelection,
        priority: Priority,
        notes: String
    ) {
        val (category, customCategoryId) = categorySelection.toEntityFields()
        transactionRepository.update(
            transaction.copy(
                amountPaise = amountPaise,
                category = category,
                customCategoryId = customCategoryId,
                priority = priority,
                notes = notes,
                status = TransactionStatus.Reviewed
            )
        )
        if (categorySelection is CategorySelection.BuiltIn) {
            transactionRepository.upsertMerchantRule(
                MerchantRuleEntity(
                    merchantKey = normalizeMerchant(transaction.merchant),
                    displayMerchant = transaction.merchant,
                    category = categorySelection.category,
                    priority = priority,
                    updatedAt = System.currentTimeMillis()
                )
            )
            checkBudgetAlerts(categorySelection.category)
        }
        cloudSyncScheduler.schedule()
    }
}
