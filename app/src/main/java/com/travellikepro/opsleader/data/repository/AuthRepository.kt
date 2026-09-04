package com.travellikepro.opsleader.data.repository

import com.travellikepro.opsleader.data.api.ApiService
import com.travellikepro.opsleader.data.api.GoogleAuthRequest
import com.travellikepro.opsleader.data.api.SignupRequest
import com.travellikepro.opsleader.data.local.datastore.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {

    suspend fun googleAuth(idToken: String) = apiService.googleAuth(
        GoogleAuthRequest(idToken = idToken)
    ).also { response ->
        if (response.isSuccessful) {
            response.body()?.let { body ->
                // sessionManager.saveTokens(body.token, ...)
            }
        }
    }

    suspend fun signup(request: SignupRequest) = apiService.signup(request)
}