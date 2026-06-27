package com.example.expensetracker.domain.usecase.transaction

import com.example.expensetracker.core.transaction.TransactionDedupe
import com.example.expensetracker.data.local.entity.TransactionEntity
import com.example.expensetracker.domain.model.ParsedTransaction
import com.example.expensetracker.domain.model.TransactionType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionDedupeTest {

    private val hdfcSms =
        "Sent Rs.5.00 From HDFC Bank A/C *4915 To aksatoskar@okicici On 25/06/26 " +
            "Ref 617606918178 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808"
    private val hdfcNotification =
        "JD-HDFCBK-S Sent Rs.5.00 From HDFC Bank A/C *4915 To aksatoskar@okicici On 25/06/26 " +
            "Ref 617606918178 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808 SIM1 137"

    @Test
    fun treatsSmsAndMirroredNotificationAsSameTransaction() {
        assertTrue(
            TransactionDedupe.isSameTransaction(
                amountA = 500L,
                rawA = hdfcSms,
                merchantA = "7308080808",
                timestampA = 1_000L,
                amountB = 500L,
                rawB = hdfcNotification,
                merchantB = "7308080808 SIM1 137",
                timestampB = 1_050L
            )
        )
    }

    @Test
    fun twoAtmWithdrawalsSameAmountMinutesApartAreNotDuplicates() {
        val sms1 =
            "Withdrawn Rs.10000 From HDFC Bank Card x5386 At +Gayatri Apt L J Road " +
                "On 2026-06-27:08:42:11 Bal Rs.58771.43 Not You? Call 18002586161/SMS BLOCK DC 5386 to 7308080808"
        val sms2 =
            "Withdrawn Rs.10000 From HDFC Bank Card x5386 At +Gayatri Apt L J Road " +
                "On 2026-06-27:08:43:19 Bal Rs.48771.43 Not You? Call 18002586161/SMS BLOCK DC 5386 to 7308080808"

        assertFalse(
            TransactionDedupe.isSameTransaction(
                amountA = 1_000_000L,
                rawA = sms1,
                merchantA = "Gayatri Apt L J Road",
                timestampA = 1_000L,
                amountB = 1_000_000L,
                rawB = sms2,
                merchantB = "Gayatri Apt L J Road",
                timestampB = 68_000L
            )
        )
    }

    @Test
    fun differentReferenceNumbersAreNotDuplicates() {
        assertFalse(
            TransactionDedupe.isSameTransaction(
                amountA = 500L,
                rawA = hdfcSms,
                merchantA = "shop",
                timestampA = 1_000L,
                amountB = 500L,
                rawB = hdfcSms.replace("617606918178", "999999999999"),
                merchantB = "shop",
                timestampB = 1_000L
            )
        )
    }

    @Test
    fun ingestMatchesExistingTransactionByReference() {
        val parsed = ParsedTransaction(
            amountPaise = 500L,
            merchant = "7308080808 SIM1 137",
            type = TransactionType.Debit,
            timestamp = 1_000L,
            source = "Notification",
            rawText = hdfcNotification
        )
        val existing = TransactionEntity(
            id = 1L,
            amountPaise = 500L,
            merchant = "7308080808",
            type = TransactionType.Debit,
            timestamp = 980L,
            source = "SMS",
            rawText = hdfcSms
        )
        assertTrue(
            TransactionDedupe.isSameTransaction(
                amountA = parsed.amountPaise,
                rawA = parsed.rawText,
                merchantA = parsed.merchant,
                timestampA = parsed.timestamp,
                amountB = existing.amountPaise,
                rawB = existing.rawText,
                merchantB = existing.merchant,
                timestampB = existing.timestamp
            )
        )
    }
}
