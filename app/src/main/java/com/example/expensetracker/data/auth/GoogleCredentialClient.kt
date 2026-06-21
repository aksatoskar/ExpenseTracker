package com.example.expensetracker.data.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves a Google ID token via the Credential Manager UI.
 *
 * Requires an [Activity] to host the account picker, so the token is fetched here (data layer) and
 * the resulting token is handed to [com.example.expensetracker.domain.auth.AuthRepository].
 */
@Singleton
class GoogleCredentialClient @Inject constructor() {

    /** Shows the Google account picker and returns the selected account's ID token. */
    suspend fun getGoogleIdToken(activity: Activity): Result<String> = runCatching {
        // Resolved by name so the project still compiles before the OAuth client is added to
        // google-services.json (enable Google sign-in + SHA-1 in Firebase Console).
        val resId = activity.resources.getIdentifier(
            "default_web_client_id", "string", activity.packageName
        )
        check(resId != 0) {
            "Google sign-in is not configured yet. Enable the Google provider and add this app's " +
                "SHA-1 in Firebase Console, then refresh google-services.json."
        }
        val serverClientId = activity.getString(resId)

        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(activity, request)
        val credential = response.credential
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Unexpected credential type: ${credential.type}")
        }
        GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
