package com.example.expensetracker.domain.usecase.category

import com.example.expensetracker.data.local.entity.CustomCategoryEntity
import com.example.expensetracker.domain.model.CategorySelection
import com.example.expensetracker.domain.repository.CustomCategoryRepository
import java.util.UUID
import javax.inject.Inject

class CreateCustomCategoryUseCase @Inject constructor(
    private val customCategoryRepository: CustomCategoryRepository
) {
    sealed class Result {
        data class Success(val selection: CategorySelection.Custom) : Result()
        data object EmptyName : Result()
        data object DuplicateName : Result()
    }

    suspend operator fun invoke(rawName: String): Result {
        val name = rawName.trim()
        if (name.isBlank()) return Result.EmptyName
        if (customCategoryRepository.nameExists(name)) return Result.DuplicateName
        val entity = CustomCategoryEntity(
            name = name,
            syncId = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis()
        )
        val id = customCategoryRepository.insert(entity)
        return Result.Success(CategorySelection.Custom(id, name))
    }
}
