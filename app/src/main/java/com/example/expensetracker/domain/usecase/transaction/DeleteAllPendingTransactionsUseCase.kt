package com.example.expensetracker.domain.usecase.transaction

import com.example.expensetracker.domain.repository.TransactionRepository
import javax.inject.Inject

/** Permanently removes every transaction still awaiting review. */
class DeleteAllPendingTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val deleteTransaction: DeleteTransactionUseCase
) {
    suspend operator fun invoke(): Int {
        val pending = transactionRepository.getPending()
        pending.forEach { deleteTransaction(it.id) }
        return pending.size
    }
}
