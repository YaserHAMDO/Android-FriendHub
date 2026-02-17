package com.engyh.friendhub.presentation.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.engyh.friendhub.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider

class GoogleSignInHelper(
    private val context: Context,
) {

    suspend fun signIn(): Result<AuthCredential> {
        return try {
            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            Result.success(extractFirebaseCredential(result))
        } catch (e: GetCredentialException) {
            Log.w("GoogleSignInHelper", "getCredential failed/cancelled", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "signIn unexpected error", e)
            Result.failure(e)
        }
    }

    private fun extractFirebaseCredential(result: GetCredentialResponse): AuthCredential {
        val credential = result.credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            return GoogleAuthProvider.getCredential(googleCred.idToken, null)
        }

        throw IllegalStateException("Unexpected credential type: ${credential::class.java}")
    }
}
