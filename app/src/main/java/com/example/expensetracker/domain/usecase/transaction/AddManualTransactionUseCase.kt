package com.example.expensetracker.domain.usecase.transaction

import com.example.expensetracker.core.money.normalizeMerchant
import com.example.expensetracker.data.local.entity.MerchantRuleEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.CategorySelection
import com.example.expensetracker.domain.model.toEntityFields
import com.example.expensetracker.domain.model.Priority
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.usecase.budget.CheckBudgetAlertsUseCase
import javax.inject.Inject

/**
 * Records a transaction the user enters by hand (already `Reviewed`), for spends the automatic
 * detection missed. Also learns the merchant rule and re-checks the budget.
 */
class AddManualTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val checkBudgetAlerts: CheckBudgetAlertsUseCase
) {
    suspend operator fun invoke(
        amountPaise: Long,
        merchant: String,
        categorySelection: CategorySelection,
        priority: Priority,
        notes: String,
        timestamp: Long
    ) {
        val (category, customCategoryId) = categorySelection.toEntityFields()
        val transaction = TransactionEntity(
            amountPaise = amountPaise,
            merchant = merchant,
            type = TransactionType.Debit,
            timestamp = timestamp,
            source = "Manual",
            rawText = "Manually added",
            category = category,
            customCategoryId = customCategoryId,
            priority = priority,
            notes = notes,
            status = TransactionStatus.Reviewed,
            notified = true
        )
        transactionRepository.insert(transaction)
        if (categorySelection is CategorySelection.BuiltIn) {
            transactionRepository.upsertMerchantRule(
                MerchantRuleEntity(
                    merchantKey = normalizeMerchant(merchant),
                    displayMerchant = merchant,
                    category = categorySelection.category,
                    priority = priority,
                    updatedAt = System.currentTimeMillis()
                )
            )
            checkBudgetAlerts(categorySelection.category)
        }
    }
}
