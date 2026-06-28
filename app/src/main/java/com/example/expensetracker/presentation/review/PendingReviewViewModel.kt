package com.example.expensetracker.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.usecase.transaction.DeleteAllPendingTransactionsUseCase
import com.example.expensetracker.domain.usecase.transaction.DeleteTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingReviewViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    private val deleteAllPendingTransactions: DeleteAllPendingTransactionsUseCase,
    private val deleteTransaction: DeleteTransactionUseCase
) : ViewModel() {

    val pending: StateFlow<List<TransactionEntity>> =
        transactionRepository.pending.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteAllPending() {
        viewModelScope.launch { deleteAllPendingTransactions() }
    }

    fun deleteSelected(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { deleteTransaction(it) }
        }
    }
}
