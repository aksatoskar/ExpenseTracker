package com.example.expensetracker.domain.usecase.report

import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.data.local.entity.MerchantRuleEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.AmountByCategory
import com.example.expensetracker.domain.model.AmountByMerchant
import com.example.expensetracker.domain.model.AmountByPriority
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SendPendingReviewReminderUseCaseTest {

    @Test
    fun `does nothing when no pending transactions`() = runBlocking {
        val notifier = RecordingNotifier()
        val useCase = SendPendingReviewReminderUseCase(
            transactionRepository = FakeTransactionRepository(pendingTransactions = emptyList()),
            notifier = notifier
        )

        useCase()

        assertNull(notifier.pendingCount)
    }

    @Test
    fun `notifies with pending count when transactions need review`() = runBlocking {
        val notifier = RecordingNotifier()
        val useCase = SendPendingReviewReminderUseCase(
            transactionRepository = FakeTransactionRepository(
                pendingTransactions = listOf(samplePending(), samplePending())
            ),
            notifier = notifier
        )

        useCase()

        assertEquals(2, notifier.pendingCount)
    }

    private fun samplePending() = TransactionEntity(
        amountPaise = 100,
        merchant = "Store",
        type = TransactionType.Debit,
        timestamp = 1L,
        source = "SMS",
        rawText = "test",
        status = TransactionStatus.PendingReview
    )
}

private class FakeTransactionRepository(
    private val pendingTransactions: List<TransactionEntity>
) : TransactionRepository {
    override val latest: Flow<List<TransactionEntity>> = flowOf(emptyList())
    override val all: Flow<List<TransactionEntity>> = flowOf(emptyList())
    override val pending: Flow<List<TransactionEntity>> = flowOf(pendingTransactions)
    override val pendingCount: Flow<Int> = flowOf(pendingTransactions.size)
    override fun observeTransaction(id: Long): Flow<TransactionEntity?> = flowOf(null)
    override suspend fun getTransaction(id: Long): TransactionEntity? = null
    override suspend fun getPending(): List<TransactionEntity> = pendingTransactions
    override suspend fun insert(transaction: TransactionEntity): Long = 0
    override suspend fun update(transaction: TransactionEntity) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun recordDeletion(syncId: String) {}
    override suspend fun findRecentAmountMatches(
        amountPaise: Long,
        type: TransactionType,
        start: Long,
        end: Long
    ): List<TransactionEntity> = emptyList()
    override suspend fun getMerchantRule(merchantKey: String): MerchantRuleEntity? = null
    override suspend fun upsertMerchantRule(rule: MerchantRuleEntity) {}
    override fun observeTotal(range: DateRange): Flow<Long> = flowOf(0)
    override fun observeCategoryTotals(range: DateRange): Flow<List<AmountByCategory>> = flowOf(emptyList())
    override fun observePriorityTotals(range: DateRange): Flow<List<AmountByPriority>> = flowOf(emptyList())
    override fun observeTopMerchants(range: DateRange, limit: Int): Flow<List<AmountByMerchant>> = flowOf(emptyList())
    override suspend fun getCategorySpent(category: Category, range: DateRange): Long = 0
    override suspend fun getDebitTransactions(range: DateRange): List<TransactionEntity> = emptyList()
}

private class RecordingNotifier : Notifier {
    var pendingCount: Int? = null

    override fun createChannels() {}
    override fun showDetected(transactionId: Long, amountPaise: Long, merchant: String, source: String) = true
    override fun showReminder(transactionId: Long, title: String, body: String) = true
    override fun showPendingReviewReminder(pendingCount: Int): Boolean {
        this.pendingCount = pendingCount
        return true
    }
    override fun showTest() = true
    override fun showBudgetAlert(category: Category, thresholdPercent: Int, spentPaise: Long, limitPaise: Long) = true
    override fun showReport(title: String, body: String) = true
    override fun cancel(transactionId: Long) {}
    override fun areNotificationsEnabled() = true
}
