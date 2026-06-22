package com.example.expensetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

/** Stores the app's enums as their stable `name` strings. */
class ExpenseConverters {
    @TypeConverter fun transactionTypeToString(value: TransactionType?) = value?.name
    @TypeConverter fun stringToTransactionType(value: String?) = value?.let(TransactionType::valueOf)
    @TypeConverter fun statusToString(value: TransactionStatus?) = value?.name
    @TypeConverter fun stringToStatus(value: String?) = value?.let(TransactionStatus::valueOf)
    @TypeConverter fun categoryToString(value: Category?) = value?.name
    @TypeConverter fun stringToCategory(value: String?) = value?.let(Category::valueOf)
    @TypeConverter fun priorityToString(value: Priority?) = value?.name
    @TypeConverter fun stringToPriority(value: String?) = value?.let(Priority::valueOf)
}

@Database(
    entities = [
        TransactionEntity::class,
        MerchantRuleEntity::class,
        BudgetEntity::class,
        MonthlyReportEntity::class,
        BudgetHistoryEntity::class,
        DeletedTransactionEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(ExpenseConverters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        const val NAME = "expense_tracker.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `budget_history` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`yearMonth` TEXT NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`limitPaise` INTEGER NOT NULL, " +
                        "`spentPaise` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_history_yearMonth_category` " +
                        "ON `budget_history` (`yearMonth`, `category`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `syncId` TEXT")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_syncId` " +
                        "ON `transactions` (`syncId`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `deleted_transactions` (" +
                        "`syncId` TEXT NOT NULL, " +
                        "`deletedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`syncId`))"
                )
            }
        }
    }
}
