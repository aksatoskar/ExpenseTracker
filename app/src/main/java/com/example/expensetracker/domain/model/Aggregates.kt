package com.example.expensetracker.domain.model

/** Summed spend grouped by [category]. `null` represents uncategorized rows. */
data class AmountByCategory(val category: Category?, val amountPaise: Long)

/** Summed spend grouped by [priority]. `null` represents rows without a priority. */
data class AmountByPriority(val priority: Priority?, val amountPaise: Long)

/** Summed spend grouped by [merchant]. */
data class AmountByMerchant(val merchant: String, val amountPaise: Long)
