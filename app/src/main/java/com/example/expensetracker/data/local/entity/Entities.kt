package com.example.expensetracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Priority
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.model.TransactionType

/**
 * A single detected or manually-added transaction. Amounts are stored in paise.
 * Reused as the shared model across layers in this pragmatic single-module architecture.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index("timestamp"), Index("merchant"), Index("status"), Index("category"),
        Index(value = ["syncId"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val merchant: String,
    val type: TransactionType,
    val timestamp: Long,
    val source: String,
    val rawText: String,
    val status: TransactionStatus = TransactionStatus.PendingReview,
    val category: Category? = null,
    val priority: Priority? = null,
    val notes: String = "",
    val notified: Boolean = false,
    /** Stable cross-device identity for cloud sync; null until first synced. */
    val syncId: String? = null
)

/**
 * A "tombstone": records that a previously-synced transaction (identified by its [syncId]) was
 * deleted, so the deletion can be propagated to the cloud and other devices instead of the record
 * being resurrected by the union merge.
 */
@Entity(tableName = "deleted_transactions")
data class DeletedTransactionEntity(
    @PrimaryKey val syncId: String,
    val deletedAt: Long
)

/** Learned mapping from a normalized merchant key to its category/priority, for auto-fill. */
@Entity(tableName = "merchant_rules", indices = [Index(value = ["merchantKey"], unique = true)])
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantKey: String,
    val displayMerchant: String,
    val category: Category,
    val priority: Priority,
    val updatedAt: Long
)

/** A monthly spending limit for a [category]; alert flags track which thresholds were notified. */
@Entity(tableName = "budgets", indices = [Index(value = ["category"], unique = true)])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: Category,
    val limitPaise: Long,
    val alert50Sent: Boolean = false,
    val alert75Sent: Boolean = false,
    val alert90Sent: Boolean = false,
    val alert100Sent: Boolean = false
)

/** Archived snapshot of a budget vs. actual spend for a past month. */
@Entity(tableName = "budget_history", indices = [Index(value = ["yearMonth", "category"], unique = true)])
data class BudgetHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val yearMonth: String,
    val category: Category,
    val limitPaise: Long,
    val spentPaise: Long,
    val createdAt: Long
)

/** Generated end-of-month report summary. */
@Entity(tableName = "monthly_reports", indices = [Index(value = ["yearMonth"], unique = true)])
data class MonthlyReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val yearMonth: String,
    val totalPaise: Long,
    val topMerchant: String,
    val essentialPaise: Long,
    val optionalPaise: Long,
    val wastefulPaise: Long,
    val savingsEstimatePaise: Long,
    val generatedAt: Long
)
