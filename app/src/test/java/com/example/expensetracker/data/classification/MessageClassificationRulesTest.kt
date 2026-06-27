package com.example.expensetracker.data.classification

import com.example.expensetracker.domain.classification.MessageClassificationInput
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
    fun acceptsFederalBankDebitSmsWithStandardSecurityFooter() {
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
        assertTrue(result!!.confidence >= 80)
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

        assertEquals(MessageType.Unknown, result?.type)
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
    fun rejectsUpcomingPaymentKeywords() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Your autopay of Rs 999 will be debited on 30-Jun-26 from A/c *1234",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis()
            )
        )

        assertEquals(MessageType.FutureDebit, result?.type)
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
    fun acceptsHdfcDebitWithTrustedSender() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Txn successful. INR 99 debited from A/c XX1234 on 20-Jun-25",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis(),
                sender = "AD-HDFCBK-S"
            )
        )

        assertEquals(MessageType.ActualDebit, result?.type)
        assertTrue(result!!.confidence >= 80)
    }

    @Test
    fun acceptsSpentOnCardMessage() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Spent Rs.1730.43 on Debit Card ending 4567 at AMAZON",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis(),
                sender = "VK-AXISBK-S"
            )
        )

        assertEquals(MessageType.ActualDebit, result?.type)
        assertTrue(result!!.confidence >= 80)
    }

    @Test
    fun acceptsExecutedStandingInstruction() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Standing instruction executed. Rs.1500 debited",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis(),
                sender = "AD-HDFCBK-S"
            )
        )

        assertEquals(MessageType.ActualDebit, result?.type)
    }

    @Test
    fun defersWeakDebitToMl() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "Rs.450 paid to Swiggy ref 12345",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis()
            )
        )

        assertNull(result)
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
    fun acceptsIciciUpiDebitWithPayeeCreditedWording() {
        val result = rules.evaluate(
            MessageClassificationInput(
                rawText = "ICICI Bank Acct XX678 debited for Rs 1190.00 on 27-Jun-26; SWIGGY credited. " +
                    "UPI:683699861026. Call 18002662 for dispute.",
                source = "SMS",
                receivedAtMillis = System.currentTimeMillis(),
                sender = "JD-ICICIT-S"
            )
        )

        assertEquals(MessageType.ActualDebit, result?.type)
        assertTrue(result!!.confidence >= 80)
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
