package com.travellikepro.opsleader.data.local.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class SessionManager {

    private val sharedPreferences: SharedPreferences?

    @Inject
    constructor(@ApplicationContext context: Context) {
        this.sharedPreferences = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "secure_session_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("ops_session_prefs", Context.MODE_PRIVATE)
        }
    }

    // Secondary no-arg constructor for unit testing
    constructor() {
        this.sharedPreferences = null
    }

    open fun saveTokens(accessToken: String, refreshToken: String? = null) {
        sharedPreferences?.edit()?.apply {
            putString("access_token", accessToken)
            if (refreshToken != null) {
                putString("refresh_token", refreshToken)
            }
            apply()
        }
    }

    open fun saveUser(
        id: String?,
        name: String?,
        email: String?,
        role: String?,
        phone: String? = null,
        region: String? = null
    ) {
        sharedPreferences?.edit()?.apply {
            putString("user_id", id)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_role", role)
            putString("user_phone", phone)
            putString("user_region", region)
            apply()
        }
    }

    open fun getAccessToken(): String? {
        return sharedPreferences?.getString("access_token", null)
    }

    open fun getRefreshToken(): String? {
        return sharedPreferences?.getString("refresh_token", null)
    }

    open fun getUserId(): String? = sharedPreferences?.getString("user_id", null)
    open fun getUserName(): String? = sharedPreferences?.getString("user_name", "Ops Leader")
    open fun getUserEmail(): String? = sharedPreferences?.getString("user_email", "")
    open fun getUserRole(): String? = sharedPreferences?.getString("user_role", "OPS_LEADER")
    open fun getUserPhone(): String? = sharedPreferences?.getString("user_phone", "")
    open fun getUserRegion(): String? = sharedPreferences?.getString("user_region", "Rajasthan")

    open fun isLoggedIn(): Boolean {
        return !getAccessToken().isNullOrBlank()
    }

    open fun clearSession() {
        sharedPreferences?.edit()?.clear()?.apply()
    }
}
