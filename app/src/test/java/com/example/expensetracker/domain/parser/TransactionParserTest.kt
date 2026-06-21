package com.example.expensetracker.domain.parser

import com.example.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionParserTest {
    private val parser = TransactionParser()

    @Test
    fun parsesUpiDebitSms() {
        val parsed = parser.parse("Rs.450 paid to Swiggy via UPI ref 12345", "SMS", 1_000L)

        assertNotNull(parsed)
        assertEquals(45_000L, parsed!!.amountPaise)
        assertEquals("Swiggy", parsed.merchant)
        assertEquals(TransactionType.Debit, parsed.type)
        assertEquals(1_000L, parsed.timestamp)
    }

    @Test
    fun ignoresCredits() {
        val parsed = parser.parse("INR 1000 credited to your account", "SMS", 1_000L)

        assertNull(parsed)
    }

    @Test
    fun parsesTransferredNotification() {
        val parsed = parser.parse("Payment successful. Rs 1200 transferred to AMAZON PAY", "Notification", 2_000L)

        assertNotNull(parsed)
        assertEquals(120_000L, parsed!!.amountPaise)
        assertEquals(TransactionType.Debit, parsed.type)
    }

    @Test
    fun parsesDebitedSmsWithoutPaidKeyword() {
        val parsed = parser.parse("Txn successful. INR 99 debited from A/c XX1234 on 20-Jun", "SMS", 3_000L)

        assertNotNull(parsed)
        assertEquals(9_900L, parsed!!.amountPaise)
        assertEquals(TransactionType.Debit, parsed.type)
    }
}
