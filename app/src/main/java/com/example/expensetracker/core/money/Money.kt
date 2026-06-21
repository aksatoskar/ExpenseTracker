package com.example.expensetracker.core.money

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToLong

/** Converts a rupee amount to integer paise, the canonical unit stored everywhere. */
fun rupeesToPaise(value: Double): Long = (value * 100.0).roundToLong()

/** Formats an amount in paise as a localized Indian Rupee string (e.g. `₹1,250`). */
fun formatInr(paise: Long): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.currency = Currency.getInstance("INR")
    format.maximumFractionDigits = if (paise % 100L == 0L) 0 else 2
    return format.format(paise / 100.0)
}

/**
 * Normalizes a raw merchant string into a stable key used for de-duplication and merchant rules.
 * Lower-cases, strips punctuation and collapses whitespace.
 */
fun normalizeMerchant(value: String): String =
    value.lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
