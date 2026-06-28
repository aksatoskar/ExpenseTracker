package com.example.expensetracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.core.money.rupeesToPaise
import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.analytics.AnalyticsEvent
import com.example.expensetracker.domain.analytics.AnalyticsTracker
import com.example.expensetracker.domain.model.CategorySelection
import com.example.expensetracker.domain.model.DashboardState
import com.example.expensetracker.domain.model.Priority
import com.example.expensetracker.domain.repository.BudgetRepository
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.usecase.dashboard.ObserveDashboardUseCase
import com.example.expensetracker.domain.usecase.transaction.AddManualTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Immutable snapshot backing the dashboard. */
data class DashboardUiState(
    val dashboard: DashboardState = DashboardState(),
    val latest: List<TransactionEntity> = emptyList(),
    val pending: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDashboard: ObserveDashboardUseCase,
    transactionRepository: TransactionRepository,
    budgetRepository: BudgetRepository,
    private val addManualTransaction: AddManualTransactionUseCase,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        observeDashboard(),
        transactionRepository.latest,
        transactionRepository.pending,
        budgetRepository.budgets
    ) { dashboard, latest, pending, budgets ->
        DashboardUiState(dashboard, latest, pending, budgets)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** Validates and persists a manually entered transaction; returns false on invalid input. */
    fun addManual(
        amountRupees: String,
        merchant: String,
        categorySelection: CategorySelection,
        priority: Priority,
        notes: String,
        timestamp: Long = System.currentTimeMillis()
    ): Boolean {
        val amount = amountRupees.toDoubleOrNull()?.takeIf { it > 0 } ?: return false
        val trimmedMerchant = merchant.trim().ifBlank { return false }
        viewModelScope.launch {
            addManualTransaction(
                amountPaise = rupeesToPaise(amount),
                merchant = trimmedMerchant,
                categorySelection = categorySelection,
                priority = priority,
                notes = notes.trim(),
                timestamp = timestamp
            )
            (categorySelection as? CategorySelection.BuiltIn)?.category?.let {
                analytics.log(AnalyticsEvent.ManualTransactionAdded(it))
            }
        }
        return true
    }
}
