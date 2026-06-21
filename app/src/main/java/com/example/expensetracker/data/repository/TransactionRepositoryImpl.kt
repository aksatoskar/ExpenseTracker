package com.example.expensetracker.data.repository

import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.data.local.ExpenseDao
import com.example.expensetracker.data.local.entity.MerchantRuleEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.AmountByCategory
import com.example.expensetracker.domain.model.AmountByMerchant
import com.example.expensetracker.domain.model.AmountByPriority
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Room-backed [TransactionRepository]; a thin pass-through with no business rules. */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao
) : TransactionRepository {

    override val latest: Flow<List<TransactionEntity>> = dao.observeLatestTransactions()
    override val all: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
    override val pending: Flow<List<TransactionEntity>> = dao.observePendingTransactions()
    override val pendingCount: Flow<Int> = dao.observePendingCount()

    override fun observeTransaction(id: Long): Flow<TransactionEntity?> = dao.observeTransaction(id)
    override suspend fun getTransaction(id: Long): TransactionEntity? = dao.getTransaction(id)
    override suspend fun getPending(): List<TransactionEntity> = dao.getPendingTransactions()

    override suspend fun insert(transaction: TransactionEntity): Long = dao.insertTransaction(transaction)
    override suspend fun update(transaction: TransactionEntity) = dao.updateTransaction(transaction)
    override suspend fun deleteById(id: Long) = dao.deleteTransaction(id)

    override suspend fun findRecentAmountMatches(
        amountPaise: Long,
        type: TransactionType,
        start: Long,
        end: Long
    ): List<TransactionEntity> = dao.findRecentAmountMatches(amountPaise, type, start, end)

    override suspend fun getMerchantRule(merchantKey: String): MerchantRuleEntity? = dao.getMerchantRule(merchantKey)
    override suspend fun upsertMerchantRule(rule: MerchantRuleEntity) = dao.upsertMerchantRule(rule)

    override fun observeTotal(range: DateRange): Flow<Long> =
        dao.observeDebitTotal(range.startMillis, range.endMillis)

    override fun observeCategoryTotals(range: DateRange): Flow<List<AmountByCategory>> =
        dao.observeCategoryTotals(range.startMillis, range.endMillis)

    override fun observePriorityTotals(range: DateRange): Flow<List<AmountByPriority>> =
        dao.observePriorityTotals(range.startMillis, range.endMillis)

    override fun observeTopMerchants(range: DateRange, limit: Int): Flow<List<AmountByMerchant>> =
        dao.observeTopMerchants(range.startMillis, range.endMillis, limit)

    override suspend fun getCategorySpent(category: Category, range: DateRange): Long =
        dao.getCategorySpent(category, range.startMillis, range.endMillis)

    override suspend fun getDebitTransactions(range: DateRange): List<TransactionEntity> =
        dao.getDebitTransactions(range.startMillis, range.endMillis)
}
