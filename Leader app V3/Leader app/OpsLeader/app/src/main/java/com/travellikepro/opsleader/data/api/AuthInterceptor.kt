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

    private val publicEndpoints = listOf(
        "/auth/login",
        "/auth/register",
        "/auth/google",
        "/auth/refresh",
        "/auth/forgot-password",
        "/auth/reset-password",
        "/auth/verify-otp"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        val isPublic = publicEndpoints.any { path.contains(it) }

        val requestBuilder = request.newBuilder()

        if (!isPublic) {
            sessionManager.getAccessToken()?.let { token ->
                if (token.isNotBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
