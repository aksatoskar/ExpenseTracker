package com.example.expensetracker.domain.model

/** Direction of money movement for a transaction. The app only acts on [Debit]s. */
enum class TransactionType { Debit, Credit }

/** Lifecycle of a detected transaction as the user triages it. */
enum class TransactionStatus { PendingReview, Reviewed, Skipped }

/** User-facing spending category with a human-readable [label]. */
enum class Category(val label: String) {
    FoodDining("Food & Dining"),
    Shopping("Shopping"),
    Travel("Travel"),
    BillsUtilities("Bills & Utilities"),
    RentHome("Rent/Home"),
    Health("Health"),
    Education("Education"),
    Investments("Investments"),
    Entertainment("Entertainment"),
    Other("Other")
}

/** How necessary a spend is, used for priority-based insights. */
enum class Priority { Essential, Optional, Wasteful }
