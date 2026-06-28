package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.local.entity.CustomCategoryEntity
import kotlinx.coroutines.flow.Flow

interface CustomCategoryRepository {
    fun observeAll(): Flow<List<CustomCategoryEntity>>
    suspend fun getAll(): List<CustomCategoryEntity>
    suspend fun getById(id: Long): CustomCategoryEntity?
    suspend fun getBySyncId(syncId: String): CustomCategoryEntity?
    suspend fun insert(entity: CustomCategoryEntity): Long
    suspend fun upsert(entity: CustomCategoryEntity)
    suspend fun nameExists(name: String): Boolean
}
