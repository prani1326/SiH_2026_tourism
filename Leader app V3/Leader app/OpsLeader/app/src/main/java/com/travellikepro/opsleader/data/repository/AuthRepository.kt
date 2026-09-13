package com.travellikepro.opsleader.data.repository

import com.travellikepro.opsleader.data.api.*
import com.travellikepro.opsleader.data.firebase.FirebaseFirestoreService
import com.travellikepro.opsleader.data.local.datastore.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val firestoreService: FirebaseFirestoreService? = null,
    private val sessionManager: SessionManager
) {
    constructor(apiService: ApiService, sessionManager: SessionManager) : this(apiService, null, sessionManager)

    suspend fun login(email: String, password: String): Result<UserResponse> {
        // 1. Try Firebase Firestore Cloud directly
        val firestoreResult = firestoreService?.login(email, password)
        if (firestoreResult != null && firestoreResult.isSuccess) {
            val user = firestoreResult.getOrNull()!!
            sessionManager.saveTokens("firebase-token-${user.id}")
            sessionManager.saveUser(
                id = user.id,
                name = user.displayName,
                email = user.email ?: email,
                role = user.role ?: "OPS_LEADER",
                phone = user.phone,
                region = user.region ?: "North India - Rajasthan & Delhi"
            )
            return Result.success(user)
        }

        // 2. Fallback to API Service if server endpoint exists
        return try {
            val response = apiService.login(LoginRequest(email = email, password = password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val authData = body.data
                val token = authData?.token ?: body.token
                val user = authData?.user ?: body.user

                if (token != null) {
                    sessionManager.saveTokens(token)
                }
                if (user != null) {
                    sessionManager.saveUser(
                        id = user.id,
                        name = user.displayName,
                        email = user.email ?: email,
                        role = user.role ?: "OPS_LEADER",
                        phone = user.phone,
                        region = user.region ?: "Rajasthan"
                    )
                    Result.success(user)
                } else {
                    Result.success(UserResponse(id = "user", name = email.substringBefore("@"), email = email, role = "OPS_LEADER"))
                }
            } else {
                // If API service also returned error, return the specific error
                val errorMsg = response.errorBody()?.string() ?: response.message()
                Result.failure(Exception(errorMsg.ifBlank { "Invalid credentials" }))
            }
        } catch (e: Exception) {
            // Return firestoreResult error or exception
            firestoreResult ?: Result.failure(e)
        }
    }

    suspend fun googleAuth(idToken: String, referenceId: String? = null): Result<AuthResponse> {
        return try {
            val response = apiService.googleAuth(GoogleAuthRequest(idToken = idToken, referenceId = referenceId))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.token != null) {
                    sessionManager.saveTokens(body.token)
                }
                if (body.user != null) {
                    sessionManager.saveUser(
                        id = body.user.id,
                        name = body.user.name,
                        email = body.user.email,
                        role = body.user.role,
                        phone = body.user.phone,
                        region = body.user.region
                    )
                }
                Result.success(body)
            } else {
                Result.failure(Exception(response.message() ?: "Google authentication failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signup(request: SignupRequest) = apiService.signup(request)

    suspend fun getMe(): Result<UserResponse> {
        return try {
            val response = apiService.getMe()
            if (response.isSuccessful && response.body()?.data != null) {
                val user = response.body()!!.data!!
                sessionManager.saveUser(
                    id = user.id,
                    name = user.name,
                    email = user.email,
                    role = user.role,
                    phone = user.phone,
                    region = user.region
                )
                Result.success(user)
            } else {
                Result.success(getSessionUser())
            }
        } catch (e: Exception) {
            Result.success(getSessionUser())
        }
    }

    suspend fun logout() {
        try {
            apiService.logout()
        } catch (_: Exception) {
            // Proceed to clear local session even if remote logout fails
        } finally {
            sessionManager.clearSession()
        }
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()
    fun getSessionUser(): UserResponse {
        return UserResponse(
            id = sessionManager.getUserId(),
            name = sessionManager.getUserName(),
            email = sessionManager.getUserEmail(),
            role = sessionManager.getUserRole(),
            phone = sessionManager.getUserPhone(),
            region = sessionManager.getUserRegion()
        )
    }
}