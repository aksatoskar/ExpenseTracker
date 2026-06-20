package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType { Debit, Credit }
enum class TransactionStatus { PendingReview, Reviewed, Skipped }
enum class Category(val label: String) {
    FoodDining("Food & Dining"),
    Shopping("Shopping"),
    Travel("Travel"),
    BillsUtilities("Bills & Utilities"),
    RentHome("Rent/Home"),
    Health("Health"),
    Education("Education"),
    Investments("Investments"),
    Entertainment("Entertainment"),
    Other("Other")
}
enum class Priority { Essential, Optional, Wasteful }

@Entity(
    tableName = "transactions",
    indices = [Index("timestamp"), Index("merchant"), Index("status"), Index("category")]
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
    val notified: Boolean = false
)

@Entity(tableName = "merchant_rules", indices = [Index(value = ["merchantKey"], unique = true)])
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantKey: String,
    val displayMerchant: String,
    val category: Category,
    val priority: Priority,
    val updatedAt: Long
)

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

@Entity(tableName = "budget_history", indices = [Index(value = ["yearMonth", "category"], unique = true)])
data class BudgetHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val yearMonth: String,
    val category: Category,
    val limitPaise: Long,
    val spentPaise: Long,
    val createdAt: Long
)

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

data class AmountByCategory(val category: Category?, val amountPaise: Long)
data class AmountByPriority(val priority: Priority?, val amountPaise: Long)
data class AmountByMerchant(val merchant: String, val amountPaise: Long)
