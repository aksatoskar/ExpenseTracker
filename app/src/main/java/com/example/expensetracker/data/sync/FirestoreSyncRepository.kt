package com.example.expensetracker.data.sync

import com.example.expensetracker.core.concurrency.DispatcherProvider
import com.example.expensetracker.data.local.ExpenseDao
import com.example.expensetracker.data.local.entity.BudgetEntity
import com.example.expensetracker.data.local.entity.BudgetHistoryEntity
import com.example.expensetracker.data.local.entity.DeletedTransactionEntity
import com.example.expensetracker.data.local.entity.MerchantRuleEntity
import com.example.expensetracker.data.local.entity.MonthlyReportEntity
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Priority
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.model.TransactionType
import com.example.expensetracker.domain.sync.SyncRepository
import com.example.expensetracker.domain.sync.SyncResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud Firestore implementation of [SyncRepository].
 *
 * Data lives under `users/{uid}/{collection}/{docId}`. Merge semantics per collection:
 * - transactions: union, deduped by a stable content-derived [TransactionEntity.syncId] (doc id).
 * - merchantRules: last-write-wins by `updatedAt` (doc id = merchantKey).
 * - budgetHistory / monthlyReports: union by natural key (immutable monthly snapshots).
 * - budgets: this device wins on conflict (doc id = category; budgets carry no timestamp).
 */
@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val dao: ExpenseDao,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : SyncRepository {

    override suspend fun sync(uid: String): SyncResult = withContext(dispatchers.io) {
        val userDoc = firestore.collection("users").document(uid)
        var pushed = 0
        var pulled = 0

        val txnCol = userDoc.collection(COL_TRANSACTIONS)

        // --- Tombstones first: merge delete markers, then purge those records everywhere so a
        // deletion is never resurrected by the union merge below. ---
        val tombstoneCol = userDoc.collection(COL_DELETED_TRANSACTIONS)
        val localTombstones = dao.getDeletedTransactions().associateBy { it.syncId }
        val remoteTombstoneDocs = tombstoneCol.get().await().documents
        val remoteTombstoneIds = remoteTombstoneDocs.map { it.id }.toSet()

        remoteTombstoneDocs.forEach { doc ->
            if (!localTombstones.containsKey(doc.id)) {
                dao.insertDeletedTransaction(
                    DeletedTransactionEntity(doc.id, doc.getLong("deletedAt") ?: System.currentTimeMillis())
                )
            }
        }
        localTombstones.values.forEach { tomb ->
            if (tomb.syncId !in remoteTombstoneIds) {
                tombstoneCol.document(tomb.syncId).set(mapOf("deletedAt" to tomb.deletedAt)).await()
            }
        }
        val tombstoneIds: Set<String> = localTombstones.keys + remoteTombstoneIds
        tombstoneIds.forEach { id ->
            txnCol.document(id).delete().await()
            dao.deleteTransactionBySyncId(id)
        }

        // --- Transactions: union by syncId, skipping anything tombstoned ---
        val localTxns = dao.getAllTransactions()
            .filter { it.syncId !in tombstoneIds }
            .map { it.ensureSyncId() }
        val localBySync = localTxns.associateBy { it.syncId!! }
        val remoteTxns = txnCol.get().await().documents
        val remoteIds = remoteTxns.map { it.id }.toSet()

        remoteTxns.forEach { doc ->
            if (!localBySync.containsKey(doc.id) && doc.id !in tombstoneIds) {
                dao.insertTransaction(doc.toTransaction())
                pulled++
            }
        }
        localTxns.forEach { txn ->
            if (txn.syncId !in remoteIds) {
                txnCol.document(txn.syncId!!).set(txn.toMap()).await()
                pushed++
            }
        }

        // --- Budgets: this device wins ---
        val budgetCol = userDoc.collection(COL_BUDGETS)
        val localBudgets = dao.getBudgets()
        val localBudgetCats = localBudgets.map { it.category }.toSet()
        val remoteBudgets = budgetCol.get().await().documents
        remoteBudgets.forEach { doc ->
            val cat = runCatching { Category.valueOf(doc.id) }.getOrNull() ?: return@forEach
            if (cat !in localBudgetCats) {
                dao.upsertBudget(doc.toBudget(cat))
                pulled++
            }
        }
        localBudgets.forEach { budget ->
            budgetCol.document(budget.category.name).set(budget.toMap()).await()
            pushed++
        }

        // --- Merchant rules: last-write-wins by updatedAt ---
        val ruleCol = userDoc.collection(COL_MERCHANT_RULES)
        val localRules = dao.getAllMerchantRules().associateBy { it.merchantKey }
        val remoteRules = ruleCol.get().await().documents.associateBy { it.id }
        (localRules.keys + remoteRules.keys).forEach { key ->
            val local = localRules[key]
            val remote = remoteRules[key]
            when {
                local != null && remote != null -> {
                    val remoteUpdated = remote.getLong("updatedAt") ?: 0L
                    if (remoteUpdated > local.updatedAt) {
                        dao.upsertMerchantRule(remote.toMerchantRule()); pulled++
                    } else {
                        ruleCol.document(key).set(local.toMap()).await(); pushed++
                    }
                }
                local != null -> { ruleCol.document(key).set(local.toMap()).await(); pushed++ }
                remote != null -> { dao.upsertMerchantRule(remote.toMerchantRule()); pulled++ }
            }
        }

        // --- Budget history: union by yearMonth+category ---
        val historyCol = userDoc.collection(COL_BUDGET_HISTORY)
        val localHistory = dao.getAllBudgetHistory()
        val localHistoryKeys = localHistory.map { "${it.yearMonth}_${it.category.name}" }.toSet()
        val remoteHistory = historyCol.get().await().documents
        remoteHistory.forEach { doc ->
            if (doc.id !in localHistoryKeys) {
                doc.toBudgetHistory()?.let { dao.upsertBudgetHistory(it); pulled++ }
            }
        }
        localHistory.forEach { h ->
            val key = "${h.yearMonth}_${h.category.name}"
            if (key !in remoteHistory.map { it.id }.toSet()) {
                historyCol.document(key).set(h.toMap()).await(); pushed++
            }
        }

        // --- Monthly reports: union by yearMonth ---
        val reportCol = userDoc.collection(COL_REPORTS)
        val localReports = dao.getAllReports()
        val localReportKeys = localReports.map { it.yearMonth }.toSet()
        val remoteReports = reportCol.get().await().documents
        remoteReports.forEach { doc ->
            if (doc.id !in localReportKeys) {
                doc.toReport()?.let { dao.upsertMonthlyReport(it); pulled++ }
            }
        }
        localReports.forEach { r ->
            if (r.yearMonth !in remoteReports.map { it.id }.toSet()) {
                reportCol.document(r.yearMonth).set(r.toMap()).await(); pushed++
            }
        }

        SyncResult(pushed, pulled)
    }

    /** Assigns a deterministic content-based syncId if missing, persisting it locally. */
    private suspend fun TransactionEntity.ensureSyncId(): TransactionEntity {
        if (syncId != null) return this
        val key = "$timestamp|$amountPaise|$merchant|${type.name}|$source|$rawText"
        val generated = UUID.nameUUIDFromBytes(key.toByteArray()).toString()
        dao.setTransactionSyncId(id, generated)
        return copy(syncId = generated)
    }

    private fun TransactionEntity.toMap(): Map<String, Any?> = mapOf(
        "amountPaise" to amountPaise,
        "merchant" to merchant,
        "type" to type.name,
        "timestamp" to timestamp,
        "source" to source,
        "rawText" to rawText,
        "status" to status.name,
        "category" to category?.name,
        "priority" to priority?.name,
        "notes" to notes,
        "notified" to notified,
        "syncId" to syncId
    )

    private fun DocumentSnapshot.toTransaction() = TransactionEntity(
        amountPaise = getLong("amountPaise") ?: 0L,
        merchant = getString("merchant").orEmpty(),
        type = getString("type")?.let { TransactionType.valueOf(it) } ?: TransactionType.Debit,
        timestamp = getLong("timestamp") ?: 0L,
        source = getString("source").orEmpty(),
        rawText = getString("rawText").orEmpty(),
        status = getString("status")?.let { TransactionStatus.valueOf(it) } ?: TransactionStatus.PendingReview,
        category = getString("category")?.let { Category.valueOf(it) },
        priority = getString("priority")?.let { Priority.valueOf(it) },
        notes = getString("notes").orEmpty(),
        notified = getBoolean("notified") ?: false,
        syncId = id
    )

    private fun BudgetEntity.toMap(): Map<String, Any?> = mapOf(
        "category" to category.name,
        "limitPaise" to limitPaise,
        "alert50Sent" to alert50Sent,
        "alert75Sent" to alert75Sent,
        "alert90Sent" to alert90Sent,
        "alert100Sent" to alert100Sent
    )

    private fun DocumentSnapshot.toBudget(category: Category) = BudgetEntity(
        category = category,
        limitPaise = getLong("limitPaise") ?: 0L,
        alert50Sent = getBoolean("alert50Sent") ?: false,
        alert75Sent = getBoolean("alert75Sent") ?: false,
        alert90Sent = getBoolean("alert90Sent") ?: false,
        alert100Sent = getBoolean("alert100Sent") ?: false
    )

    private fun MerchantRuleEntity.toMap(): Map<String, Any?> = mapOf(
        "merchantKey" to merchantKey,
        "displayMerchant" to displayMerchant,
        "category" to category.name,
        "priority" to priority.name,
        "updatedAt" to updatedAt
    )

    private fun DocumentSnapshot.toMerchantRule() = MerchantRuleEntity(
        merchantKey = getString("merchantKey") ?: id,
        displayMerchant = getString("displayMerchant").orEmpty(),
        category = getString("category")?.let { Category.valueOf(it) } ?: Category.Other,
        priority = getString("priority")?.let { Priority.valueOf(it) } ?: Priority.Optional,
        updatedAt = getLong("updatedAt") ?: 0L
    )

    private fun BudgetHistoryEntity.toMap(): Map<String, Any?> = mapOf(
        "yearMonth" to yearMonth,
        "category" to category.name,
        "limitPaise" to limitPaise,
        "spentPaise" to spentPaise,
        "createdAt" to createdAt
    )

    private fun DocumentSnapshot.toBudgetHistory(): BudgetHistoryEntity? {
        val category = getString("category")?.let { runCatching { Category.valueOf(it) }.getOrNull() } ?: return null
        return BudgetHistoryEntity(
            yearMonth = getString("yearMonth").orEmpty(),
            category = category,
            limitPaise = getLong("limitPaise") ?: 0L,
            spentPaise = getLong("spentPaise") ?: 0L,
            createdAt = getLong("createdAt") ?: 0L
        )
    }

    private fun MonthlyReportEntity.toMap(): Map<String, Any?> = mapOf(
        "yearMonth" to yearMonth,
        "totalPaise" to totalPaise,
        "topMerchant" to topMerchant,
        "essentialPaise" to essentialPaise,
        "optionalPaise" to optionalPaise,
        "wastefulPaise" to wastefulPaise,
        "savingsEstimatePaise" to savingsEstimatePaise,
        "generatedAt" to generatedAt
    )

    private fun DocumentSnapshot.toReport(): MonthlyReportEntity? {
        val yearMonth = getString("yearMonth") ?: return null
        return MonthlyReportEntity(
            yearMonth = yearMonth,
            totalPaise = getLong("totalPaise") ?: 0L,
            topMerchant = getString("topMerchant").orEmpty(),
            essentialPaise = getLong("essentialPaise") ?: 0L,
            optionalPaise = getLong("optionalPaise") ?: 0L,
            wastefulPaise = getLong("wastefulPaise") ?: 0L,
            savingsEstimatePaise = getLong("savingsEstimatePaise") ?: 0L,
            generatedAt = getLong("generatedAt") ?: 0L
        )
    }

    private companion object {
        const val COL_TRANSACTIONS = "transactions"
        const val COL_DELETED_TRANSACTIONS = "deletedTransactions"
        const val COL_BUDGETS = "budgets"
        const val COL_MERCHANT_RULES = "merchantRules"
        const val COL_BUDGET_HISTORY = "budgetHistory"
        const val COL_REPORTS = "monthlyReports"
    }
}
