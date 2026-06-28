package com.example.expensetracker.data.repository

import com.example.expensetracker.data.local.ExpenseDao
import com.example.expensetracker.data.local.entity.CustomCategoryEntity
import com.example.expensetracker.domain.repository.CustomCategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomCategoryRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao
) : CustomCategoryRepository {

    override fun observeAll(): Flow<List<CustomCategoryEntity>> = dao.observeCustomCategories()

    override suspend fun getAll(): List<CustomCategoryEntity> = dao.getCustomCategories()

    override suspend fun getById(id: Long): CustomCategoryEntity? = dao.getCustomCategory(id)

    override suspend fun getBySyncId(syncId: String): CustomCategoryEntity? =
        dao.getCustomCategoryBySyncId(syncId)

    override suspend fun insert(entity: CustomCategoryEntity): Long = dao.insertCustomCategory(entity)

    override suspend fun upsert(entity: CustomCategoryEntity) {
        dao.upsertCustomCategory(entity)
    }

    override suspend fun nameExists(name: String): Boolean =
        dao.customCategoryNameExists(name.trim()) > 0
}
