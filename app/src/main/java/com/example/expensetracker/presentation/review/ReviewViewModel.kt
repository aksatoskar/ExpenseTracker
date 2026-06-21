package com.example.expensetracker.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Priority
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.usecase.transaction.DeleteTransactionUseCase
import com.example.expensetracker.domain.usecase.transaction.SaveReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the review dialog: observe a transaction, confirm it, or delete it. */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val saveReview: SaveReviewUseCase,
    private val deleteTransaction: DeleteTransactionUseCase
) : ViewModel() {

    fun observeTransaction(id: Long): Flow<TransactionEntity?> = transactionRepository.observeTransaction(id)

    fun save(transaction: TransactionEntity, amountPaise: Long, category: Category, priority: Priority, notes: String) {
        viewModelScope.launch { saveReview(transaction, amountPaise, category, priority, notes) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { deleteTransaction(id) }
    }
}
