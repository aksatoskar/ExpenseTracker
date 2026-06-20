package com.example.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        BudgetHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(ExpenseConverters::class)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile private var instance: ExpenseDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
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

        fun get(context: Context): ExpenseDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
