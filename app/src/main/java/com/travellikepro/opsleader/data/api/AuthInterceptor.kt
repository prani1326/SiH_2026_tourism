package com.travellikepro.opsleader.data.api

import com.travellikepro.opsleader.data.local.datastore.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // Skip auth headers for /auth endpoints
        if (!chain.request().url.encodedPath.contains("/auth/")) {
            sessionManager.getAccessToken()?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
