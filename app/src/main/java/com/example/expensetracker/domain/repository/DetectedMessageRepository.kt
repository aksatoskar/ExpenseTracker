package com.example.expensetracker.domain.repository

import com.example.expensetracker.data.local.entity.DetectedMessageEntity
import kotlinx.coroutines.flow.Flow

/** Persistence for SMS/notification bodies the parser classified as debit transactions. */
interface DetectedMessageRepository {
    val messages: Flow<List<DetectedMessageEntity>>
    val count: Flow<Int>

    suspend fun insert(message: DetectedMessageEntity): Long
    suspend fun hasNearDuplicate(rawText: String, timestamp: Long, windowMillis: Long): Boolean
    suspend fun deleteById(id: Long)
    suspend fun clearAll()
}
