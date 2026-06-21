package com.example.expensetracker.domain.model

/** A raw inbox SMS body with its received [timestamp] (epoch millis). */
data class RawSms(val body: String, val timestamp: Long)
