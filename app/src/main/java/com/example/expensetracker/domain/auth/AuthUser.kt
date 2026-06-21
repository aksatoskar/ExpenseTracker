package com.example.expensetracker.domain.auth

/** Minimal authenticated-user profile exposed to the app, decoupled from the auth SDK. */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)
