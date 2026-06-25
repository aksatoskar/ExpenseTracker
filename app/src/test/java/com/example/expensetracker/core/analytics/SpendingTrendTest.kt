package com.example.expensetracker.core.analytics

import com.example.expensetracker.core.time.DateRange
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.TransactionStatus
import com.example.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class SpendingTrendTest {

    @Test
    fun `compute buckets spend across equal time windows`() {
        val range = DateRange(0, 99)
        val transactions = listOf(
            txn(10, 100),
            txn(30, 200),
            txn(70, 300)
        )
        val buckets = SpendingTrend.compute(transactions, range, bucketCount = 2)
        assertEquals(listOf(300L, 300L), buckets)
    }

    @Test
    fun `bucket count matches selected range label`() {
        assertEquals(6, SpendingTrend.bucketCount("Today"))
        assertEquals(7, SpendingTrend.bucketCount("Week"))
        assertEquals(5, SpendingTrend.bucketCount("Month"))
    }

    private fun txn(timestamp: Long, amountPaise: Long) = TransactionEntity(
        id = timestamp,
        amountPaise = amountPaise,
        merchant = "Test",
        timestamp = timestamp,
        type = TransactionType.Debit,
        source = "Test",
        rawText = "Test",
        status = TransactionStatus.Reviewed
    )
}
