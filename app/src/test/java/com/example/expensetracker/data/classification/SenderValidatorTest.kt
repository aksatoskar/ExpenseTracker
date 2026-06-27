package com.example.expensetracker.data.classification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SenderValidatorTest {
    private lateinit var validator: SenderValidator

    @Before
    fun setUp() {
        validator = SenderValidator(DefaultClassificationConfigRepository())
    }

    @Test
    fun recognizesDltBankSenderWithoutWhitelist() {
        assertTrue(validator.isDltSender("JD-ICICIT-S"))
        assertTrue(validator.isDltSender("AD-HDFCBK-S"))
        assertTrue(validator.isLikelyBankMessage("VM-NEWBNK-T", "New Bank Acct XX1 debited for Rs 100"))
    }

    @Test
    fun recognizesBankFromMessageBodyWhenCodeIsUnknown() {
        assertTrue(
            validator.isLikelyBankMessage(
                sender = "TX-RGNLBK-S",
                messageBody = "Regional Grameen Bank A/c *1234 debited for Rs 500 on 27-Jun-26"
            )
        )
    }

    @Test
    fun recognizesIciciFromBodyAndDltHeader() {
        assertTrue(
            validator.isLikelyBankMessage(
                sender = "JD-ICICIT-S",
                messageBody = "ICICI Bank Acct XX678 debited for Rs 1190.00 on 27-Jun-26; SWIGGY credited."
            )
        )
    }

    @Test
    fun rejectsRandomPhoneNumberSenderWithoutBankBody() {
        assertFalse(validator.isDltSender("+919876543210"))
        assertFalse(
            validator.isLikelyBankMessage(
                sender = "+919876543210",
                messageBody = "Congratulations you won Rs 500000"
            )
        )
    }

    @Test
    fun recognizesPaymentAppDltCode() {
        assertTrue(validator.isLikelyPaymentAppSender("PHONEPE"))
        assertTrue(validator.isLikelyPaymentAppSender("AX-PAYTM-S"))
    }
}
