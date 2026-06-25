package com.example.expensetracker.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PendingReviewViewModel @Inject constructor(
    transactionRepository: TransactionRepository
) : ViewModel() {

    val pending: StateFlow<List<TransactionEntity>> =
        transactionRepository.pending.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
