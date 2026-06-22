package com.example.expensetracker.domain.repository

import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.data.local.entity.MerchantRuleEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.AmountByCategory
import com.example.expensetracker.domain.model.AmountByMerchant
import com.example.expensetracker.domain.model.AmountByPriority
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for transactions, their derived totals and learned merchant rules.
 * Implemented by the data layer; consumed only through use cases (DIP).
 */
interface TransactionRepository {
    val latest: Flow<List<TransactionEntity>>
    val all: Flow<List<TransactionEntity>>
    val pending: Flow<List<TransactionEntity>>
    val pendingCount: Flow<Int>

    fun observeTransaction(id: Long): Flow<TransactionEntity?>
    suspend fun getTransaction(id: Long): TransactionEntity?
    suspend fun getPending(): List<TransactionEntity>

    suspend fun insert(transaction: TransactionEntity): Long
    suspend fun update(transaction: TransactionEntity)
    suspend fun deleteById(id: Long)

    /** Records a tombstone so a deleted, already-synced transaction is removed from the cloud too. */
    suspend fun recordDeletion(syncId: String)

    suspend fun findRecentAmountMatches(
        amountPaise: Long,
        type: TransactionType,
        start: Long,
        end: Long
    ): List<TransactionEntity>

    suspend fun getMerchantRule(merchantKey: String): MerchantRuleEntity?
    suspend fun upsertMerchantRule(rule: MerchantRuleEntity)

    fun observeTotal(range: DateRange): Flow<Long>
    fun observeCategoryTotals(range: DateRange): Flow<List<AmountByCategory>>
    fun observePriorityTotals(range: DateRange): Flow<List<AmountByPriority>>
    fun observeTopMerchants(range: DateRange, limit: Int = 5): Flow<List<AmountByMerchant>>

    suspend fun getCategorySpent(category: Category, range: DateRange): Long
    suspend fun getDebitTransactions(range: DateRange): List<TransactionEntity>
}
