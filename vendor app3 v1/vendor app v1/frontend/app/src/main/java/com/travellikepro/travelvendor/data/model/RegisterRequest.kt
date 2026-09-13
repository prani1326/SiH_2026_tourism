package com.travellikepro.travelvendor.data.model

data class RegisterRequest(
    val name: String,
    val email: String,
    val mobile: String,
    val password: String
)
