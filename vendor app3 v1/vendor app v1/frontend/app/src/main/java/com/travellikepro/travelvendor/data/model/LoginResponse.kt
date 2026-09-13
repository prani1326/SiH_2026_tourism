package com.travellikepro.travelvendor.data.model

data class LoginResponse(
    val success: Boolean,
    val message: String? = null,
    val data: LoginData? = null
)

data class LoginData(
    val token: String? = null,
    val vendor: Vendor? = null
)