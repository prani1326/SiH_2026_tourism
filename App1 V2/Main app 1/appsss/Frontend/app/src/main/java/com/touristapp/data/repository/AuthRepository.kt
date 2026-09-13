package com.touristapp.data.repository

import android.app.Activity
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.touristapp.data.firebase.FirestoreCollections
import com.touristapp.data.firebase.PhoneAuthCallback
import com.touristapp.data.firebase.PhoneAuthManager
import com.touristapp.data.models.UserDto
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val sessionRepository: SessionRepository,
    private val sessionManager: SessionManager,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val phoneAuthManager: PhoneAuthManager = PhoneAuthManager(auth)
) {

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun loginWithCredential(credential: AuthCredential): Result<UserDto> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null after credential sign in")
            val uid = firebaseUser.uid

            val userDoc = firestore.collection(FirestoreCollections.USERS).document(uid).get().await()
            val userDto = if (userDoc.exists()) {
                if (userDoc.getBoolean("has_seen_walkthrough") == true) {
                    sessionRepository.saveWalkthroughSeen(true)
                }
                if (userDoc.getBoolean("has_completed_onboarding") == true) {
                    sessionRepository.saveOnboardingStatus(true)
                }
                UserDto.fromFirestore(userDoc)
            } else {
                val newUser = UserDto(
                    id = uid,
                    email = firebaseUser.email,
                    full_name = firebaseUser.displayName ?: firebaseUser.phoneNumber ?: "Traveler",
                    phone = firebaseUser.phoneNumber,
                    role = "tourist"
                )
                firestore.collection(FirestoreCollections.USERS).document(uid).set(newUser.toFirestoreMap()).await()
                newUser
            }

            sessionRepository.setGuestMode(false)
            sessionManager.saveUser(userDto)

            Result.success(userDto)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun sendPhoneOtp(
        activity: Activity,
        phoneNumber: String,
        callback: PhoneAuthCallback,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null
    ) {
        phoneAuthManager.sendVerificationCode(
            activity = activity,
            phoneNumber = phoneNumber,
            callback = callback,
            resendToken = resendToken
        )
    }

    suspend fun verifyPhoneOtp(
        verificationId: String,
        code: String
    ): Result<UserDto> {
        return try {
            val credential = phoneAuthManager.getCredential(verificationId, code)
            loginWithCredential(credential)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<UserDto> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null after successful login")
            val uid = firebaseUser.uid

            // Fetch or create user document in Firestore
            val userDoc = firestore.collection(FirestoreCollections.USERS).document(uid).get().await()
            val userDto = if (userDoc.exists()) {
                if (userDoc.getBoolean("has_seen_walkthrough") == true) {
                    sessionRepository.saveWalkthroughSeen(true)
                }
                if (userDoc.getBoolean("has_completed_onboarding") == true) {
                    sessionRepository.saveOnboardingStatus(true)
                }
                UserDto.fromFirestore(userDoc)
            } else {
                val newUser = UserDto(
                    id = uid,
                    email = firebaseUser.email,
                    full_name = firebaseUser.displayName ?: "Traveler",
                    role = "tourist"
                )
                firestore.collection(FirestoreCollections.USERS).document(uid).set(newUser.toFirestoreMap()).await()
                newUser
            }

            sessionRepository.setGuestMode(false)
            sessionManager.saveUser(userDto)

            Result.success(userDto)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun signup(
        name: String,
        email: String,
        password: String
    ): Result<UserDto> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Firebase user is null after registration")
            val uid = firebaseUser.uid

            // Create user profile in Firestore
            val userDto = UserDto(
                id = uid,
                email = email.trim(),
                full_name = name.trim(),
                role = "tourist"
            )

            firestore.collection(FirestoreCollections.USERS)
                .document(uid)
                .set(userDto.toFirestoreMap())
                .await()

            sessionRepository.setGuestMode(false)
            sessionManager.saveUser(userDto)

            Result.success(userDto)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun continueAsGuest(): Result<Unit> {
        return try {
            if (auth.currentUser == null) {
                try {
                    auth.signInAnonymously().await()
                } catch (_: Exception) {}
            }
            sessionRepository.setGuestMode(true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            auth.signOut()
            sessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}