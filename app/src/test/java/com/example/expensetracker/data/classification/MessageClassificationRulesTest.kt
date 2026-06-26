package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageClassificationRulesTest {
    private val rules = MessageClassificationRules()

    @Test
    fun rejectsFederalBankPhishingSms() {
        val receivedAt = java.time.LocalDate.of(2026, 6, 25)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Debited Rs 6.00 from a/c X2685 on 25Jun26 23:59 via UPI to aksatoskar-1. " +
                    "Ref 617625008733.Bal Rs 11662.38. Not you?Call 18004251199 -Federal Bank",
                source = "SMS",
                receivedAtMillis = receivedAt,
                sender = "VA-FEDBNK-T"
            )
        )

        assertEquals(MessageLabel.Spam, result?.label)
    }

    @Test
    fun rejectsMessagingAppBankSmsMirror() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "JM-HDFCBK-S Sent Rs.6.00 From HDFC Bank A/C *4915 To AKSHAY MEGHASHYAM SATOSKA " +
                    "On 25/06/26 Ref 654207042327 Not You? Call 18002586161",
                source = "Notification",
                receivedAtMillis = 1_750_000_000_000L,
                notificationPackage = "com.google.android.apps.messaging"
            )
        )

        assertEquals(MessageLabel.Invalid, result?.label)
    }

    @Test
    fun rejectsFutureTransactionDate() {
        val receivedAt = java.time.LocalDate.of(2025, 6, 25)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Debited Rs.6.00 From HDFC Bank A/C *4915 To MERCHANT On 25/06/26 Ref 654207042327",
                source = "SMS",
                receivedAtMillis = receivedAt
            )
        )

        assertEquals(MessageLabel.Invalid, result?.label)
    }

    @Test
    fun rejectsUpcomingPaymentKeywords() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Your autopay of Rs 999 will be debited on 30-Jun-26 from A/c *1234",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis()
            )
        )

        assertEquals(MessageLabel.Invalid, result?.label)
        assertEquals("upcoming_payment", result?.reason)
    }

    @Test
    fun rejectsPurchaseReceiptKeywords() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Thank you for your purchase of Rs 450. Download your bill from the app.",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis()
            )
        )

        assertEquals(MessageLabel.Invalid, result?.label)
    }

    @Test
    fun rejectsWeakDebitWithoutStrongKeyword() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Rs.450 paid to Swiggy via UPI ref 12345",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis()
            )
        )

        assertEquals(MessageLabel.Invalid, result?.label)
        assertEquals("missing_strong_debit_keyword", result?.reason)
    }

    @Test
    fun allowsTypicalDebitSmsWithStrongKeyword() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Txn successful. INR 99 debited from A/c XX1234 on 20-Jun-25",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis()
            )
        )

        assertEquals(null, result)
    }

    @Test
    fun rejectsDebitMessageThatAlsoContainsInvoiceKeyword() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Rs 500 debited for tax invoice payment to MERCHANT",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis()
            )
        )

        assertEquals(MessageLabel.Invalid, result?.label)
        assertEquals("purchase_receipt", result?.reason)
    }
}
