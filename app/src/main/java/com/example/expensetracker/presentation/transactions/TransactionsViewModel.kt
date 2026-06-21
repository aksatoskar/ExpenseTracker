package com.example.expensetracker.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Sort options for the transactions list. */
enum class TxnSort(val label: String) {
    DateNewest("Newest first"),
    DateOldest("Oldest first"),
    AmountHigh("Amount: High to Low"),
    AmountLow("Amount: Low to High")
}

/** Time-window filter for the transactions list. */
enum class TxnPeriod(val label: String) {
    All("All"),
    Today("Today"),
    Week("Week"),
    Month("Month"),
    Year("Year")
}

/** Immutable snapshot of the filtered/sorted transactions list and the active filters. */
data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val totalPaise: Long = 0,
    val query: String = "",
    val sort: TxnSort = TxnSort.DateNewest,
    val period: TxnPeriod = TxnPeriod.All,
    val category: Category? = null
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    transactionRepository: TransactionRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(TxnSort.DateNewest)
    private val period = MutableStateFlow(TxnPeriod.All)
    private val category = MutableStateFlow<Category?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.all,
        query,
        sort,
        period,
        category
    ) { all, query, sort, period, category ->
        val periodStart = periodStartMillis(period)
        val filtered = all.asSequence()
            .filter { it.merchant.contains(query, ignoreCase = true) }
            .filter { period == TxnPeriod.All || it.timestamp >= periodStart }
            .filter { category == null || it.category == category }
            .toList()
            .let { list ->
                when (sort) {
                    TxnSort.DateNewest -> list.sortedByDescending { it.timestamp }
                    TxnSort.DateOldest -> list.sortedBy { it.timestamp }
                    TxnSort.AmountHigh -> list.sortedByDescending { it.amountPaise }
                    TxnSort.AmountLow -> list.sortedBy { it.amountPaise }
                }
            }
        TransactionsUiState(
            transactions = filtered,
            totalPaise = filtered.sumOf { it.amountPaise },
            query = query,
            sort = sort,
            period = period,
            category = category
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    fun setQuery(value: String) { query.value = value }
    fun setSort(value: TxnSort) { sort.value = value }
    fun setPeriod(value: TxnPeriod) { period.value = value }
    fun setCategory(value: Category?) { category.value = value }

    private companion object {
        fun periodStartMillis(period: TxnPeriod): Long {
            if (period == TxnPeriod.All) return Long.MIN_VALUE
            val today = LocalDate.now()
            val date = when (period) {
                TxnPeriod.Today -> today
                TxnPeriod.Week -> today.minusDays((today.dayOfWeek.value - 1).toLong())
                TxnPeriod.Month -> today.withDayOfMonth(1)
                TxnPeriod.Year -> today.withDayOfYear(1)
                TxnPeriod.All -> today
            }
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
}
