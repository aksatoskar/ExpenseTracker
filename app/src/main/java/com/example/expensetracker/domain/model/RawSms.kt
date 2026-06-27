package com.example.expensetracker.domain.model

/** A raw inbox SMS with sender, body and received [timestamp] (epoch millis). */
data class RawSms(
    val body: String,
    val timestamp: Long,
    val address: String? = null
)
