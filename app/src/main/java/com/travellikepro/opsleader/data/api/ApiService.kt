package com.travellikepro.opsleader.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class GoogleAuthRequest(
    val idToken: String,
    val referenceId: String? = null
)

data class AuthResponse(
    val success: Boolean,
    val message: String?,
    val errorCode: String?,
    val token: String?,
    val user: UserResponse?
)

data class UserResponse(
    val id: String?,
    val name: String?,
    val email: String?,
    val role: String?,
    val photoUrl: String?
)

data class SignupRequest(
    val name: String,
    val email: String,
    val referenceId: String,
    val googleIdToken: String? = null,
    val role: String? = null
)

data class SignupResponse(
    val success: Boolean,
    val message: String?,
    val errorCode: String?,
    val user: UserResponse?
)

interface ApiService {

    @POST("auth/google")
    suspend fun googleAuth(
        @Body request: GoogleAuthRequest
    ): Response<AuthResponse>

    @POST("auth/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<SignupResponse>
}