package com.touristapp.data.firebase

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider

class GoogleAuthManager(
    private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    fun getServerClientId(): String {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId)
        } else {
            ""
        }
    }

    suspend fun getGoogleAuthCredential(clientId: String? = null): Result<AuthCredential> {
        val serverClientId = clientId?.ifBlank { null } ?: getServerClientId()

        if (serverClientId.isBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Google Web Client ID not found. Please enable Google provider in Firebase Console and update google-services.json."
                )
            )
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                Result.success(authCredential)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type received: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Google Sign-In cancelled by user."))
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Google Sign-In error: ${e.localizedMessage ?: e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
