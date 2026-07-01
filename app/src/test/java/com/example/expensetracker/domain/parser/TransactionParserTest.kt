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
    fun parsesIciciUpiDebitWithPayeeCreditedWording() {
        val parsed = parser.parse(
            "ICICI Bank Acct XX678 debited for Rs 1190.00 on 27-Jun-26; SWIGGY credited. " +
                "UPI:683699861026. Call 18002662 for dispute. SMS BLOCK 678 to 9215676766",
            "SMS",
            1_000L
        )

        assertNotNull(parsed)
        assertEquals(119_000L, parsed!!.amountPaise)
        assertEquals("SWIGGY", parsed.merchant)
        assertEquals(TransactionType.Debit, parsed.type)
    }

    @Test
    fun parsesHdfcSentFromAccountDebitSmsWithUpiPayee() {
        val parsed = parser.parse(
            "Sent Rs.18000.00 From HDFC Bank A/C *4915 To mr.mayurdeshpande-4@okaxi On 30/06/26 " +
                "Ref 618173294378 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808",
            "SMS",
            1_000L
        )

        assertNotNull(parsed)
        assertEquals(1_800_000L, parsed!!.amountPaise)
        assertEquals("mr.mayurdeshpande-4@okaxi", parsed.merchant)
        assertEquals(TransactionType.Debit, parsed.type)
    }

    @Test
    fun parsesHdfcAchDebitWithInfoLine() {
        val parsed = parser.parse(
            "UPDATE: INR 20,000.00 debited from HDFC Bank XX4915 on 01-JUL-26. " +
                "Info: ACH D- Indian Clearing Corp-0000Q512XZ4X. Avl bal:INR 2,69,143.43",
            "SMS",
            1_000L
        )

        assertNotNull(parsed)
        assertEquals(2_000_000L, parsed!!.amountPaise)
        assertEquals("Indian Clearing Corp", parsed.merchant)
        assertEquals(TransactionType.Debit, parsed.type)
    }

    @Test
    fun parsesHdfcSentFromAccountDebitSms() {
        val parsed = parser.parse(
            "Sent Rs.3580.00 From HDFC Bank A/C *4915 To SHREEDEVA FOODS On 28/06/26 " +
                "Ref 125451627257 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808",
            "SMS",
            1_000L
        )

        assertNotNull(parsed)
        assertEquals(358_000L, parsed!!.amountPaise)
        assertEquals("SHREEDEVA FOODS", parsed.merchant)
        assertEquals(TransactionType.Debit, parsed.type)
    }

    @Test
    fun parsesIciciSmallUpiDebit() {
        val parsed = parser.parse(
            "ICICI Bank Acct XX678 debited for Rs 26.26 on 27-Jun-26; Pronto credited. " +
                "UPI:731640811815. Call 18002662 for dispute.",
            "SMS",
            2_000L
        )

        assertNotNull(parsed)
        assertEquals(2_626L, parsed!!.amountPaise)
        assertEquals("Pronto", parsed.merchant)
    }
}
