package com.example.expensetracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.data.local.entity.BudgetHistoryEntity
import com.example.expensetracker.data.local.entity.DeletedTransactionEntity
import com.example.expensetracker.data.local.entity.MerchantRuleEntity
import com.example.expensetracker.data.local.entity.MonthlyReportEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.AmountByCategory
import com.example.expensetracker.domain.model.AmountByMerchant
import com.example.expensetracker.domain.model.AmountByPriority
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Room data-access object. All display/analytics queries are scoped to `status = 'Reviewed'` so
 * only user-confirmed transactions feed totals, budgets and history.
 */
@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncId = :syncId LIMIT 1")
    suspend fun getTransactionBySyncId(syncId: String): TransactionEntity?

    @Query("UPDATE transactions SET syncId = :syncId WHERE id = :id")
    suspend fun setTransactionSyncId(id: Long, syncId: String)

    @Query("DELETE FROM transactions WHERE syncId = :syncId")
    suspend fun deleteTransactionBySyncId(syncId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedTransaction(tombstone: DeletedTransactionEntity)

    @Query("SELECT * FROM deleted_transactions")
    suspend fun getDeletedTransactions(): List<DeletedTransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun observeTransaction(id: Long): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE status = 'Reviewed' ORDER BY timestamp DESC LIMIT 10")
    fun observeLatestTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'Reviewed' ORDER BY timestamp DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'PendingReview' ORDER BY timestamp DESC")
    fun observePendingTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'PendingReview' ORDER BY timestamp DESC")
    suspend fun getPendingTransactions(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE status = 'PendingReview'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions WHERE type = 'Debit' AND status = 'Reviewed' AND timestamp BETWEEN :start AND :end")
    fun observeDebitTotal(start: Long, end: Long): Flow<Long>

    @Query("SELECT category, COALESCE(SUM(amountPaise), 0) AS amountPaise FROM transactions WHERE type = 'Debit' AND status = 'Reviewed' AND timestamp BETWEEN :start AND :end GROUP BY category ORDER BY amountPaise DESC")
    fun observeCategoryTotals(start: Long, end: Long): Flow<List<AmountByCategory>>

    @Query("SELECT priority, COALESCE(SUM(amountPaise), 0) AS amountPaise FROM transactions WHERE type = 'Debit' AND status = 'Reviewed' AND timestamp BETWEEN :start AND :end GROUP BY priority")
    fun observePriorityTotals(start: Long, end: Long): Flow<List<AmountByPriority>>

    @Query("SELECT merchant, COALESCE(SUM(amountPaise), 0) AS amountPaise FROM transactions WHERE type = 'Debit' AND status = 'Reviewed' AND timestamp BETWEEN :start AND :end GROUP BY merchant ORDER BY amountPaise DESC LIMIT :limit")
    fun observeTopMerchants(start: Long, end: Long, limit: Int): Flow<List<AmountByMerchant>>

    @Query("SELECT * FROM transactions WHERE type = 'Debit' AND status = 'Reviewed' AND timestamp BETWEEN :start AND :end")
    suspend fun getDebitTransactions(start: Long, end: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE amountPaise = :amountPaise AND type = :type AND timestamp BETWEEN :start AND :end")
    suspend fun findRecentAmountMatches(
        amountPaise: Long,
        type: TransactionType,
        start: Long,
        end: Long
    ): List<TransactionEntity>

    @Query("SELECT * FROM budgets ORDER BY category")
    suspend fun getBudgets(): List<BudgetEntity>

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getBudget(category: Category): BudgetEntity?

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions WHERE type = 'Debit' AND status = 'Reviewed' AND category = :category AND timestamp BETWEEN :start AND :end")
    suspend fun getCategorySpent(category: Category, start: Long, end: Long): Long

    @Query("SELECT * FROM merchant_rules WHERE merchantKey = :merchantKey LIMIT 1")
    suspend fun getMerchantRule(merchantKey: String): MerchantRuleEntity?

    @Query("SELECT * FROM merchant_rules")
    suspend fun getAllMerchantRules(): List<MerchantRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMerchantRule(rule: MerchantRuleEntity)

    @Query("SELECT * FROM budgets ORDER BY category")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonthlyReport(report: MonthlyReportEntity)

    @Query("SELECT * FROM monthly_reports ORDER BY yearMonth DESC")
    fun observeReports(): Flow<List<MonthlyReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgetHistory(history: BudgetHistoryEntity)

    @Query("SELECT * FROM budget_history ORDER BY yearMonth DESC, category ASC")
    fun observeBudgetHistory(): Flow<List<BudgetHistoryEntity>>

    @Query("SELECT * FROM budget_history")
    suspend fun getAllBudgetHistory(): List<BudgetHistoryEntity>

    @Query("SELECT * FROM monthly_reports")
    suspend fun getAllReports(): List<MonthlyReportEntity>

    @Query("SELECT COUNT(*) FROM budget_history WHERE yearMonth = :yearMonth")
    suspend fun budgetHistoryCount(yearMonth: String): Int
}
