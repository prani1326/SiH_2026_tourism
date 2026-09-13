package com.touristapp.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.touristapp.data.firebase.FirestoreCollections
import com.touristapp.data.models.UserDto
import com.touristapp.data.models.UserProfileDto
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val sessionManager: SessionManager,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun getCurrentUid(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getUserProfile(uid: String): Result<UserDto> {
        return try {
            val doc = firestore.collection(FirestoreCollections.USERS).document(uid).get().await()
            if (doc.exists()) {
                val user = UserDto.fromFirestore(doc)
                sessionManager.saveUser(user)
                Result.success(user)
            } else {
                Result.failure(NoSuchElementException("User profile does not exist in Firestore"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        uid: String,
        fullName: String,
        bio: String? = null,
        nationality: String? = null,
        language: String? = null,
        phone: String? = null,
        currency: String? = null
    ): Result<UserDto> {
        return try {
            val updates = mutableMapOf<String, Any?>()
            if (fullName.isNotBlank()) updates["full_name"] = fullName
            if (phone != null) updates["phone"] = phone

            val profileUpdates = mutableMapOf<String, Any?>()
            bio?.let { profileUpdates["bio"] = it }
            nationality?.let { profileUpdates["nationality"] = it }
            language?.let { profileUpdates["language"] = it }
            currency?.let { profileUpdates["currency"] = it }

            if (profileUpdates.isNotEmpty()) {
                updates["profile"] = profileUpdates
            }
            updates["updated_at"] = com.google.firebase.Timestamp.now()

            firestore.collection(FirestoreCollections.USERS)
                .document(uid)
                .set(updates, SetOptions.merge())
                .await()

            // Fetch refreshed profile
            getUserProfile(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        uid: String,
        destinations: List<String>,
        styles: List<String>,
        interests: List<String>
    ): Result<Unit> {
        return try {
            val preferencesMap = mapOf(
                "preferred_destinations" to destinations,
                "travel_styles" to styles,
                "interests" to interests
            )

            firestore.collection(FirestoreCollections.USERS)
                .document(uid)
                .set(mapOf("preferences" to preferencesMap), SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(
        context: Context,
        uid: String,
        uri: Uri
    ): Result<String> {
        return try {
            val storageRef = storage.reference.child("users/$uid/avatar.jpg")
            
            // Upload file to Firebase Storage
            val uploadTask = storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Save download URL to Firestore user document
            val avatarUpdate = mapOf(
                "profile.avatar_url" to downloadUrl,
                "updated_at" to com.google.firebase.Timestamp.now()
            )

            firestore.collection(FirestoreCollections.USERS)
                .document(uid)
                .update(avatarUpdate)
                .await()

            // Update in-memory session
            val currentUser = sessionManager.currentUser.value
            if (currentUser != null) {
                val updatedProfile = (currentUser.profile ?: UserProfileDto()).copy(avatar_url = downloadUrl)
                sessionManager.saveUser(currentUser.copy(profile = updatedProfile))
            }

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveFcmToken(uid: String, token: String): Result<Unit> {
        return try {
            firestore.collection(FirestoreCollections.USERS)
                .document(uid)
                .set(
                    mapOf(
                        "fcm_token" to token,
                        "fcm_updated_at" to com.google.firebase.Timestamp.now()
                    ),
                    SetOptions.merge()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
