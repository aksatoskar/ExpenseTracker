package com.example.expensetracker.domain.usecase.budget

import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.data.local.entity.BudgetHistoryEntity
import com.example.expensetracker.data.local.entity.MerchantRuleEntity
import com.example.expensetracker.data.local.entity.MonthlyReportEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.AmountByCategory
import com.example.expensetracker.domain.model.AmountByMerchant
import com.example.expensetracker.domain.model.AmountByPriority
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.BudgetRepository
import com.example.expensetracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckBudgetAlertsUseCaseTest {

    @Test
    fun firesHighestNewlyCrossedThreshold() = runBlocking {
        val budget = BudgetEntity(id = 1, category = Category.FoodDining, limitPaise = 100_00)
        val budgetRepo = FakeBudgetRepository(budget)
        val txnRepo = FakeTransactionRepository(spentPaise = 80_00) // 80% of limit
        val notifier = RecordingNotifier()

        CheckBudgetAlertsUseCase(budgetRepo, txnRepo, notifier).invoke(Category.FoodDining)

        assertEquals(75, notifier.lastThreshold)
        val saved = budgetRepo.saved!!
        assertTrue(saved.alert50Sent)
        assertTrue(saved.alert75Sent)
        assertTrue(!saved.alert90Sent)
        assertTrue(!saved.alert100Sent)
    }

    @Test
    fun resetsFlagsAndStaysSilentBelowFiftyPercent() = runBlocking {
        val budget = BudgetEntity(
            id = 1, category = Category.FoodDining, limitPaise = 100_00,
            alert50Sent = true, alert75Sent = true
        )
        val budgetRepo = FakeBudgetRepository(budget)
        val txnRepo = FakeTransactionRepository(spentPaise = 10_00) // 10%
        val notifier = RecordingNotifier()

        CheckBudgetAlertsUseCase(budgetRepo, txnRepo, notifier).invoke(Category.FoodDining)

        assertNull(notifier.lastThreshold)
        val saved = budgetRepo.saved!!
        assertTrue(!saved.alert50Sent && !saved.alert75Sent && !saved.alert90Sent && !saved.alert100Sent)
    }
}

private class FakeBudgetRepository(private var current: BudgetEntity?) : BudgetRepository {
    var saved: BudgetEntity? = null
    override val budgets: Flow<List<BudgetEntity>> = flowOf(emptyList())
    override val budgetHistory: Flow<List<BudgetHistoryEntity>> = flowOf(emptyList())
    override val reports: Flow<List<MonthlyReportEntity>> = flowOf(emptyList())
    override suspend fun getBudget(category: Category): BudgetEntity? = current
    override suspend fun getBudgets(): List<BudgetEntity> = listOfNotNull(current)
    override suspend fun upsertBudget(budget: BudgetEntity) { current = budget; saved = budget }
    override suspend fun updateBudget(budget: BudgetEntity) { current = budget; saved = budget }
    override suspend fun deleteBudget(id: Long) {}
    override suspend fun upsertBudgetHistory(history: BudgetHistoryEntity) {}
    override suspend fun upsertMonthlyReport(report: MonthlyReportEntity) {}
}

private class FakeTransactionRepository(private val spentPaise: Long) : TransactionRepository {
    override val latest: Flow<List<TransactionEntity>> = flowOf(emptyList())
    override val all: Flow<List<TransactionEntity>> = flowOf(emptyList())
    override val pending: Flow<List<TransactionEntity>> = flowOf(emptyList())
    override val pendingCount: Flow<Int> = flowOf(0)
    override fun observeTransaction(id: Long): Flow<TransactionEntity?> = flowOf(null)
    override suspend fun getTransaction(id: Long): TransactionEntity? = null
    override suspend fun getPending(): List<TransactionEntity> = emptyList()
    override suspend fun insert(transaction: TransactionEntity): Long = 0
    override suspend fun update(transaction: TransactionEntity) {}
    override suspend fun deleteById(id: Long) {}
    override suspend fun findRecentAmountMatches(amountPaise: Long, type: TransactionType, start: Long, end: Long): List<TransactionEntity> = emptyList()
    override suspend fun getMerchantRule(merchantKey: String): MerchantRuleEntity? = null
    override suspend fun upsertMerchantRule(rule: MerchantRuleEntity) {}
    override fun observeTotal(range: DateRange): Flow<Long> = flowOf(0)
    override fun observeCategoryTotals(range: DateRange): Flow<List<AmountByCategory>> = flowOf(emptyList())
    override fun observePriorityTotals(range: DateRange): Flow<List<AmountByPriority>> = flowOf(emptyList())
    override fun observeTopMerchants(range: DateRange, limit: Int): Flow<List<AmountByMerchant>> = flowOf(emptyList())
    override suspend fun getCategorySpent(category: Category, range: DateRange): Long = spentPaise
    override suspend fun getDebitTransactions(range: DateRange): List<TransactionEntity> = emptyList()
}

private class RecordingNotifier : Notifier {
    var lastThreshold: Int? = null
    override fun createChannels() {}
    override fun showDetected(transactionId: Long, amountPaise: Long, merchant: String, source: String): Boolean = true
    override fun showReminder(transactionId: Long, title: String, body: String): Boolean = true
    override fun showTest(): Boolean = true
    override fun showBudgetAlert(category: Category, thresholdPercent: Int, spentPaise: Long, limitPaise: Long): Boolean {
        lastThreshold = thresholdPercent
        return true
    }
    override fun showReport(title: String, body: String): Boolean = true
    override fun cancel(transactionId: Long) {}
    override fun areNotificationsEnabled(): Boolean = true
}
