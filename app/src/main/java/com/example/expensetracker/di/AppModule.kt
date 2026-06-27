package com.example.expensetracker.di

import com.example.expensetracker.core.concurrency.DefaultDispatcherProvider
import com.example.expensetracker.core.concurrency.DispatcherProvider
import com.example.expensetracker.data.classification.FirebaseClassificationConfigRepository
import com.example.expensetracker.data.feature.FirebaseFeatureFlagsRepository
import com.example.expensetracker.data.identity.FirebaseInstallationIdRepository
import com.example.expensetracker.data.notification.AndroidNotifier
import com.example.expensetracker.data.repository.BudgetRepositoryImpl
import com.example.expensetracker.data.repository.DetectedMessageRepositoryImpl
import com.example.expensetracker.data.repository.SettingsRepositoryImpl
import com.example.expensetracker.data.repository.SmsRepositoryImpl
import com.example.expensetracker.data.repository.TransactionRepositoryImpl
import com.example.expensetracker.domain.notification.Notifier
import com.example.expensetracker.domain.repository.BudgetRepository
import com.example.expensetracker.domain.repository.DetectedMessageRepository
import com.example.expensetracker.domain.repository.ClassificationConfigRepository
import com.example.expensetracker.domain.repository.FeatureFlagsRepository
import com.example.expensetracker.domain.repository.InstallationIdRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.repository.SmsRepository
import com.example.expensetracker.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds domain abstractions (repositories, notifier, dispatchers) to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindDetectedMessageRepository(impl: DetectedMessageRepositoryImpl): DetectedMessageRepository

    @Binds
    @Singleton
    abstract fun bindSmsRepository(impl: SmsRepositoryImpl): SmsRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindInstallationIdRepository(impl: FirebaseInstallationIdRepository): InstallationIdRepository

    @Binds
    @Singleton
    abstract fun bindFeatureFlagsRepository(impl: FirebaseFeatureFlagsRepository): FeatureFlagsRepository

    @Binds
    @Singleton
    abstract fun bindClassificationConfigRepository(
        impl: FirebaseClassificationConfigRepository
    ): ClassificationConfigRepository

    @Binds
    @Singleton
    abstract fun bindNotifier(impl: AndroidNotifier): Notifier

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider
}
