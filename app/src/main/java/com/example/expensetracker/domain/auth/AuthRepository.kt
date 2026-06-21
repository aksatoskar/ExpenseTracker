package com.example.expensetracker.domain.auth

import kotlinx.coroutines.flow.Flow

/**
 * Authentication boundary. Implemented by the data layer with Firebase Auth; consumers depend only
 * on this interface (DIP). Obtaining the Google ID token (Credential Manager UI) is handled
 * separately by the presentation layer and passed into [signInWithGoogle].
 */
interface AuthRepository {

    /** Emits the current signed-in user, or null when signed out. */
    val currentUser: Flow<AuthUser?>

    /** Returns the signed-in user synchronously, or null. */
    fun currentUserOrNull(): AuthUser?

    /** Exchanges a Google ID token for a Firebase session. */
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    /** Signs the user out of Firebase. */
    suspend fun signOut()
}
