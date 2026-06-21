package com.example.expensetracker.di

import com.example.expensetracker.data.analytics.FirebaseAnalyticsTracker
import com.example.expensetracker.data.crash.FirebaseCrashReporter
import com.example.expensetracker.domain.analytics.AnalyticsTracker
import com.example.expensetracker.domain.crash.CrashReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the analytics and crash-reporting abstractions to their Firebase implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(impl: FirebaseAnalyticsTracker): AnalyticsTracker

    @Binds
    @Singleton
    abstract fun bindCrashReporter(impl: FirebaseCrashReporter): CrashReporter
}
