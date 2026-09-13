package com.travellikepro.travelvendor.data.repository

import android.content.Context
import com.travellikepro.travelvendor.data.api.RetrofitClient
import com.travellikepro.travelvendor.data.model.LoginRequest

class AuthRepository(context: Context) {

    private val api = RetrofitClient.getApi(context)

    suspend fun login(identifier: String, password: String) =
        api.login(LoginRequest(identifier = identifier, password = password))

    suspend fun register(request: com.travellikepro.travelvendor.data.model.RegisterRequest) =
        api.register(request)

    suspend fun getMe() = api.getMe()
}