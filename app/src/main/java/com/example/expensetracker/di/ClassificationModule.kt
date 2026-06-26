package com.example.expensetracker.di

import com.example.expensetracker.data.classification.TfliteMessageClassifier
import com.example.expensetracker.domain.classification.TransactionMessageClassifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClassificationModule {

    @Binds
    @Singleton
    abstract fun bindTransactionMessageClassifier(impl: TfliteMessageClassifier): TransactionMessageClassifier
}
