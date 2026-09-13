package com.travellikepro.travelvendor.data.api

import com.travellikepro.travelvendor.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(
        chain: Interceptor.Chain
    ): Response {

        val token = tokenManager.getToken()

        val requestBuilder = chain
            .request()
            .newBuilder()

        if (!token.isNullOrBlank()) {

            requestBuilder.addHeader(
                "Authorization",
                "Bearer $token"
            )
        }

        return chain.proceed(
            requestBuilder.build()
        )
    }
}