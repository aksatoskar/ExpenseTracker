package com.example.expensetracker.di

import com.example.expensetracker.data.auth.FirebaseAuthRepository
import com.example.expensetracker.data.sync.FirestoreSyncRepository
import com.example.expensetracker.domain.auth.AuthRepository
import com.example.expensetracker.domain.sync.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds the authentication and cloud-sync abstractions to their Firebase implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: FirestoreSyncRepository): SyncRepository
}
