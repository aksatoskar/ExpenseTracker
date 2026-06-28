package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.data.local.entity.BudgetHistoryEntity
import com.example.expensetracker.data.local.entity.MonthlyReportEntity
import com.example.expensetracker.domain.model.Category
import kotlinx.coroutines.flow.Flow

/** Persistence boundary for budgets, their monthly archive history and generated reports. */
interface BudgetRepository {
    val budgets: Flow<List<BudgetEntity>>
    val budgetHistory: Flow<List<BudgetHistoryEntity>>
    val reports: Flow<List<MonthlyReportEntity>>

    suspend fun getBudget(category: Category): BudgetEntity?
    suspend fun getBudgetById(id: Long): BudgetEntity?
    suspend fun getBudgets(): List<BudgetEntity>
    suspend fun upsertBudget(budget: BudgetEntity)
    suspend fun updateBudget(budget: BudgetEntity)
    suspend fun deleteBudget(id: Long)
    suspend fun markBudgetDeleted(category: Category)
    suspend fun clearBudgetDeleted(category: Category)

    suspend fun upsertBudgetHistory(history: BudgetHistoryEntity)
    suspend fun upsertMonthlyReport(report: MonthlyReportEntity)
}
