package com.travellikepro.opsleader.data.api

import com.travellikepro.opsleader.data.local.datastore.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // If we get a 401 on the refresh token endpoint itself, give up and logout
        if (response.request.url.encodedPath.contains("/auth/refresh")) {
            sessionManager.clearSession()
            return null
        }

        synchronized(this) {
            // Attempt to refresh the token synchronously here in a real implementation
            // For now, if we get 401, we clear the session (force logout)
            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken != null) {
                // val newToken = refreshApiService.refreshToken(refreshToken).execute()
                // if (newToken.isSuccessful) {
                //    sessionManager.saveTokens(...)
                //    return response.request.newBuilder().header("Authorization", "Bearer ${newToken.body()}").build()
                // }
            }

            sessionManager.clearSession()
            return null
        }
    }
}
