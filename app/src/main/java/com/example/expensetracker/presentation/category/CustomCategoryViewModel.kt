package com.example.expensetracker.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.entity.CustomCategoryEntity
import com.example.expensetracker.domain.model.CategorySelection
import com.example.expensetracker.domain.repository.CustomCategoryRepository
import com.example.expensetracker.domain.usecase.category.CreateCustomCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomCategoryViewModel @Inject constructor(
    customCategoryRepository: CustomCategoryRepository,
    private val createCustomCategory: CreateCustomCategoryUseCase
) : ViewModel() {

    val customCategories: StateFlow<List<CustomCategoryEntity>> =
        customCategoryRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, onResult: (CreateCustomCategoryUseCase.Result) -> Unit) {
        viewModelScope.launch {
            onResult(createCustomCategory(name))
        }
    }
}
