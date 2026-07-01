package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.MessageClassificationInput
import com.example.expensetracker.domain.classification.MessageClassificationResult
import com.example.expensetracker.domain.classification.MessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MessageClassificationRulesTest {
    private lateinit var configRepository: DefaultClassificationConfigRepository
    private lateinit var senderValidator: SenderValidator
    private lateinit var rules: MessageClassificationRules

    @Before
    fun setUp() {
        configRepository = DefaultClassificationConfigRepository()
        senderValidator = SenderValidator(configRepository)
        rules = MessageClassificationRules(configRepository, senderValidator)
    }

    @Test
    fun classifiesFederalBankDebitAsActualDebit() {
        val receivedAt = java.time.LocalDate.of(2026, 6, 27)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Debited Rs 2.00 from a/c X2685 on 27Jun26 00:20 via UPI to aksatoskar-1. " +
                    "Ref 617817479920.Bal Rs 11660.38. Not you?Call 18004251199 -Federal Bank",
                source = "SMS",
                receivedAtMillis = receivedAt,
                sender = "VA-FEDBNK-T"
            )
        )

        assertEquals(MessageType.ActualDebit, result?.type)
        assertTrue(result!!.confidence >= MessageClassificationResult.NOTIFY_THRESHOLD)
        assertEquals("confirmed_bank_debit", result.reason)
    }

    @Test
    fun confirmsActualDebitForMessagingAppBankSms() {
        val receivedAt = java.time.LocalDate.of(2026, 6, 30)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Sent Rs.18000.00 From HDFC Bank A/C *4915 To mr.mayurdeshpande-4@okaxi " +
                    "On 30/06/26 Ref 618173294378 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808",
                source = "Notification",
                receivedAtMillis = receivedAt,
                notificationPackage = "com.google.android.apps.messaging"
            )
        )

        assertEquals(MessageType.ActualDebit, result?.type)
        assertTrue(result!!.confidence >= MessageClassificationResult.NOTIFY_THRESHOLD)
        assertEquals("confirmed_bank_debit", result.reason)
    }

    @Test
    fun confirmsActualDebitForMessagingAppBankSmsWithSenderPrefix() {
        val receivedAt = java.time.LocalDate.of(2026, 6, 25)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "JM-HDFCBK-S Sent Rs.6.00 From HDFC Bank A/C *4915 To AKSHAY MEGASHYAM SATOSKA " +
                    "On 25/06/26 Ref 654207042327 Not You? Call 18002586161",
                source = "Notification",
                receivedAtMillis = receivedAt,
                notificationPackage = "com.google.android.apps.messaging"
            )
        )

        assertEquals(MessageType.ActualDebit, result?.type)
        assertTrue(result!!.confidence >= MessageClassificationResult.NOTIFY_THRESHOLD)
    }

    @Test
    fun confirmsActualDebitForHdfcAchDebitSms() {
        val receivedAt = java.time.LocalDate.of(2026, 7, 1)
            .atTime(9, 17)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "UPDATE: INR 20,000.00 debited from HDFC Bank XX4915 on 01-JUL-26. " +
                    "Info: ACH D- Indian Clearing Corp-0000Q512XZ4X. Avl bal:INR 2,69,143.43",
                source = "SMS",
                receivedAtMillis = receivedAt,
                sender = "VM-HDFCBK-S"
            )
        )

        assertEquals(MessageType.ActualDebit, result?.type)
        assertTrue(result!!.confidence >= MessageClassificationResult.NOTIFY_THRESHOLD)
        assertEquals("confirmed_bank_debit", result.reason)
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
                receivedAtMillis = receivedAt,
                sender = "AD-HDFCBK-S"
            )
        )

        assertEquals(MessageType.FutureDebit, result?.type)
    }

    @Test
    fun rejectsGrowwSipReminderAsFutureDebit() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "SIP: Instalment due in 2 days ₹20,000.00 will be deducted for " +
                    "Parag Parikh Flexi Cap Fund Direct Growth. Please ensure sufficient bank balance.",
                source = "Notification",
                receivedAtMillis = System.currentTimeMillis(),
                notificationPackage = "com.nextbillion.groww"
            )
        )

        assertEquals(MessageType.FutureDebit, result?.type)
        assertEquals("future_debit_keyword", result?.reason)
    }

    @Test
    fun rejectsPharmacyReceiptWithSender() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Thank you for your purchase of Rs 450. Download your bill from the app.",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis(),
                sender = "AX-APLPHR-S"
            )
        )

        assertEquals(MessageType.Receipt, result?.type)
    }

    @Test
    fun rejectsOtpMessage() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "OTP 123456 for transaction of Rs 500",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis()
            )
        )

        assertEquals(MessageType.Otp, result?.type)
    }

    @Test
    fun rejectsCreditMessage() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Salary of Rs 55000 credited",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis()
            )
        )

        assertEquals(MessageType.Credit, result?.type)
    }

    @Test
    fun rejectsBitLyPhishingEvenWithDebitKeyword() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Federal Bank alert: unusual activity. Update PAN at bit.ly/fake-link",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis(),
                sender = "VA-FEDBNK-T"
            )
        )

        assertEquals(MessageType.PhishingSpam, result?.type)
    }
}
