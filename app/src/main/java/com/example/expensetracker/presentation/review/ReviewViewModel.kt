package com.example.expensetracker.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.analytics.AnalyticsEvent
import com.example.expensetracker.domain.analytics.AnalyticsTracker
import com.example.expensetracker.domain.model.CategorySelection
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
    private val deleteTransaction: DeleteTransactionUseCase,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    fun observeTransaction(id: Long): Flow<TransactionEntity?> = transactionRepository.observeTransaction(id)

    fun save(transaction: TransactionEntity, amountPaise: Long, categorySelection: CategorySelection, priority: Priority, notes: String) {
        viewModelScope.launch {
            saveReview(transaction, amountPaise, categorySelection, priority, notes)
            val analyticsCategory = (categorySelection as? CategorySelection.BuiltIn)?.category
            if (analyticsCategory != null) {
                analytics.log(AnalyticsEvent.ReviewSaved(analyticsCategory))
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            deleteTransaction(id)
            analytics.log(AnalyticsEvent.TransactionDeleted)
        }
    }
}
