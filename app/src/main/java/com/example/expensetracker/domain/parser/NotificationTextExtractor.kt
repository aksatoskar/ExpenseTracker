package com.example.expensetracker.domain.parser

import android.os.Bundle

/**
 * Flattens the relevant text fields of a notification's [Bundle] extras into a single string
 * suitable for [TransactionParser]. Covers standard title/text keys, messaging-style payloads,
 * inbox text lines and any custom CharSequence extras.
 */
object NotificationTextExtractor {

    private val textKeys = listOf(
        "android.title",
        "android.text",
        "android.subText",
        "android.infoText",
        "android.summaryText",
        "android.bigText"
    )

    fun extract(extras: Bundle): String {
        val parts = linkedSetOf<String>()

        textKeys.forEach { key ->
            extras.getCharSequence(key)?.toString()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        }

        extras.getParcelableArray("android.messages")?.forEach { parcel ->
            if (parcel is Bundle) {
                parcel.getCharSequence("text")?.toString()?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            }
        }

        @Suppress("DEPRECATION")
        extras.get("android.textLines")?.let { lines ->
            when (lines) {
                is Array<*> -> lines.filterIsInstance<CharSequence>().forEach { parts.add(it.toString()) }
                is List<*> -> lines.filterIsInstance<CharSequence>().forEach { parts.add(it.toString()) }
            }
        }

        extras.keySet().forEach { key ->
            if (key.startsWith("android.") || key.startsWith("androidx.")) return@forEach
            when (val value = extras.get(key)) {
                is CharSequence -> value.toString().takeIf { it.isNotBlank() }?.let { parts.add(it) }
                is Array<*> -> value.filterIsInstance<CharSequence>().forEach { parts.add(it.toString()) }
            }
        }

        return parts.joinToString(" ")
    }
}
