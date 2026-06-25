package com.example.expensetracker.data.repository

import com.example.expensetracker.data.local.ExpenseDao
import com.example.expensetracker.data.local.entity.DetectedMessageEntity
import com.example.expensetracker.domain.repository.DetectedMessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetectedMessageRepositoryImpl @Inject constructor(
    private val dao: ExpenseDao
) : DetectedMessageRepository {

    override val messages: Flow<List<DetectedMessageEntity>> = dao.observeDetectedMessages()
    override val count: Flow<Int> = dao.observeDetectedMessageCount()

    override suspend fun insert(message: DetectedMessageEntity): Long =
        dao.insertDetectedMessage(message)

    override     suspend fun hasNearDuplicate(rawText: String, timestamp: Long, windowMillis: Long): Boolean =
        dao.hasDetectedMessage(rawText, timestamp - windowMillis, timestamp + windowMillis)

    override suspend fun getRecentMessages(timestamp: Long, windowMillis: Long): List<DetectedMessageEntity> =
        dao.getDetectedMessagesBetween(timestamp - windowMillis, timestamp + windowMillis)

    override suspend fun deleteById(id: Long) = dao.deleteDetectedMessage(id)

    override suspend fun clearAll() = dao.clearDetectedMessages()
}
