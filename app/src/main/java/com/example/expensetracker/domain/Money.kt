package com.example.expensetracker.domain

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

fun rupeesToPaise(value: Double): Long = (value * 100.0).roundToLong()

fun formatInr(paise: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")
    format.maximumFractionDigits = if (paise % 100L == 0L) 0 else 2
    return format.format(paise / 100.0)
}

fun normalizeMerchant(value: String): String =
    value.lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
