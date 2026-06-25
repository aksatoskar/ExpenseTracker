package com.example.expensetracker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.entity.DetectedMessageEntity
import com.example.expensetracker.domain.repository.DetectedMessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetectedMessagesViewModel @Inject constructor(
    private val repository: DetectedMessageRepository
) : ViewModel() {

    val messages: StateFlow<List<DetectedMessageEntity>> =
        repository.messages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }
}
