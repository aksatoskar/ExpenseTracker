package com.example.expensetracker.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.domain.model.AnalyticsState
import com.example.expensetracker.domain.usecase.analytics.ObserveAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Drives the analytics tab, switching the observed [DateRange] as the user changes the range tab. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    observeAnalytics: ObserveAnalyticsUseCase
) : ViewModel() {

    private val selected = MutableStateFlow("Month")
    val selectedRange: StateFlow<String> = selected.asStateFlow()

    val analytics: StateFlow<AnalyticsState> = selected
        .flatMapLatest { label -> observeAnalytics(rangeFor(label), label) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsState())

    fun setRange(label: String) { selected.value = label }

    private fun rangeFor(label: String): DateRange = when (label) {
        "Today" -> DateRange.today()
        "Week" -> DateRange.week()
        else -> DateRange.month()
    }
}
