package com.example.expensetracker.core.concurrency

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over the coroutine dispatchers used by the app.
 *
 * Injecting dispatchers (instead of referencing [Dispatchers] directly) keeps domain and data
 * code framework-agnostic and lets unit tests substitute deterministic test dispatchers.
 */
interface DispatcherProvider {
    /** Dispatcher for disk / database / network I/O. */
    val io: CoroutineDispatcher

    /** Dispatcher for CPU-bound work. */
    val default: CoroutineDispatcher

    /** Main/UI thread dispatcher. */
    val main: CoroutineDispatcher
}

/** Production [DispatcherProvider] backed by [Dispatchers]. */
@Singleton
class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}
