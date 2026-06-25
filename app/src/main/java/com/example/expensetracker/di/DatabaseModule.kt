package com.example.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.example.expensetracker.data.local.ExpenseDao
import com.example.expensetracker.data.local.ExpenseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the Room database and its DAO as application-scoped singletons. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExpenseDatabase =
        Room.databaseBuilder(context, ExpenseDatabase::class.java, ExpenseDatabase.NAME)
            .addMigrations(
                ExpenseDatabase.MIGRATION_1_2,
                ExpenseDatabase.MIGRATION_2_3,
                ExpenseDatabase.MIGRATION_3_4,
                ExpenseDatabase.MIGRATION_4_5
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideExpenseDao(database: ExpenseDatabase): ExpenseDao = database.expenseDao()
}
