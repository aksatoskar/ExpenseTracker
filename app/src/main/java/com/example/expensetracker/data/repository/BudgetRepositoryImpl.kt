package com.example.expensetracker.data.repository

import com.example.expensetracker.data.local.ExpenseDao
import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.data.local.entity.BudgetHistoryEntity
import com.example.expensetracker.data.local.entity.DeletedBudgetEntity
import com.example.expensetracker.data.local.entity.MonthlyReportEntity
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed [BudgetRepository]. */
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao
) : BudgetRepository {

    override val budgets: Flow<List<BudgetEntity>> = dao.observeBudgets()
    override val budgetHistory: Flow<List<BudgetHistoryEntity>> = dao.observeBudgetHistory()
    override val reports: Flow<List<MonthlyReportEntity>> = dao.observeReports()

    override suspend fun getBudget(category: Category): BudgetEntity? = dao.getBudget(category)
    override suspend fun getBudgetById(id: Long): BudgetEntity? = dao.getBudgetById(id)
    override suspend fun getBudgets(): List<BudgetEntity> = dao.getBudgets()
    override suspend fun upsertBudget(budget: BudgetEntity) = dao.upsertBudget(budget)
    override suspend fun updateBudget(budget: BudgetEntity) = dao.updateBudget(budget)
    override suspend fun deleteBudget(id: Long) = dao.deleteBudget(id)

    override suspend fun markBudgetDeleted(category: Category) {
        dao.insertDeletedBudget(
            DeletedBudgetEntity(category = category, deletedAt = System.currentTimeMillis())
        )
    }

    override suspend fun clearBudgetDeleted(category: Category) = dao.clearDeletedBudget(category)

    override suspend fun upsertBudgetHistory(history: BudgetHistoryEntity) = dao.upsertBudgetHistory(history)
    override suspend fun upsertMonthlyReport(report: MonthlyReportEntity) = dao.upsertMonthlyReport(report)
}
