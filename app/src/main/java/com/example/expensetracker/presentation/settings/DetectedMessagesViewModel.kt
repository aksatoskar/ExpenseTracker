package com.example.expensetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.entity.DetectedMessageEntity
import com.example.expensetracker.domain.repository.DetectedMessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DetectedMessagesTab { Today, Past }

@HiltViewModel
class DetectedMessagesViewModel @Inject constructor(
    private val repository: DetectedMessageRepository
) : ViewModel() {

    val todayMessages: StateFlow<List<DetectedMessageEntity>> =
        repository.todayMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _pastMessages = MutableStateFlow<List<DetectedMessageEntity>>(emptyList())
    val pastMessages: StateFlow<List<DetectedMessageEntity>> = _pastMessages.asStateFlow()

    private val _pastLoading = MutableStateFlow(false)
    val pastLoading: StateFlow<Boolean> = _pastLoading.asStateFlow()

    private val _pastHasMore = MutableStateFlow(true)
    val pastHasMore: StateFlow<Boolean> = _pastHasMore.asStateFlow()

    private val _selectedTab = MutableStateFlow(DetectedMessagesTab.Today)
    val selectedTab: StateFlow<DetectedMessagesTab> = _selectedTab.asStateFlow()

    private var pastPage = 0

    val hasAnyMessages: StateFlow<Boolean> =
        repository.count
            .map { it > 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun selectTab(tab: DetectedMessagesTab) {
        _selectedTab.value = tab
        if (tab == DetectedMessagesTab.Past && _pastMessages.value.isEmpty() && _pastHasMore.value) {
            loadNextPastPage()
        }
    }

    fun loadNextPastPage() {
        if (_pastLoading.value || !_pastHasMore.value) return
        viewModelScope.launch {
            _pastLoading.value = true
            val page = repository.getPastMessages(page = pastPage, pageSize = PAGE_SIZE)
            _pastMessages.update { current -> current + page }
            pastPage++
            _pastHasMore.value = page.size == PAGE_SIZE
            _pastLoading.value = false
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            _pastMessages.update { messages -> messages.filterNot { it.id == id } }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            resetPastPagination()
        }
    }

    private fun resetPastPagination() {
        pastPage = 0
        _pastMessages.value = emptyList()
        _pastHasMore.value = true
        _pastLoading.value = false
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
