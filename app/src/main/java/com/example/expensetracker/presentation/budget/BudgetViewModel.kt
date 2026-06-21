package com.example.expensetracker.presentation.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.core.money.rupeesToPaise
import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.data.local.entity.BudgetHistoryEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.analytics.AnalyticsEvent
import com.example.expensetracker.domain.analytics.AnalyticsTracker
import com.example.expensetracker.domain.model.AmountByCategory
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.repository.BudgetRepository
import com.example.expensetracker.domain.repository.TransactionRepository
import com.example.expensetracker.domain.usecase.budget.DeleteBudgetUseCase
import com.example.expensetracker.domain.usecase.budget.UpsertBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shared by the budget manage/summary tabs and the per-category detail overlay. */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    budgetRepository: BudgetRepository,
    transactionRepository: TransactionRepository,
    private val upsertBudget: UpsertBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    val budgets: StateFlow<List<BudgetEntity>> =
        budgetRepository.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val budgetHistory: StateFlow<List<BudgetHistoryEntity>> =
        budgetRepository.budgetHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthCategoryTotals: StateFlow<List<AmountByCategory>> =
        transactionRepository.observeCategoryTotals(DateRange.month())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> =
        transactionRepository.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveBudget(category: Category, amountRupees: String, existing: BudgetEntity?) {
        val amount = amountRupees.toDoubleOrNull() ?: return
        viewModelScope.launch {
            upsertBudget(category, rupeesToPaise(amount), existing)
            analytics.log(AnalyticsEvent.BudgetSet(category))
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            deleteBudgetUseCase(budget.id)
            analytics.log(AnalyticsEvent.BudgetDeleted(budget.category))
        }
    }
}
