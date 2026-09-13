package com.travellikepro.travelvendor.utils

import android.content.Context

class TokenManager(context: Context) {

    private val preferences =
        context.getSharedPreferences(
            "vendor_preferences",
            Context.MODE_PRIVATE
        )

    fun saveToken(token: String) {
        preferences.edit()
            .putString("token", token)
            .apply()
    }

    fun getToken(): String? {
        return preferences.getString("token", null)
    }

    fun clearToken() {
        preferences.edit()
            .remove("token")
            .apply()
    }
}