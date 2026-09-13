package com.travellikepro.travelvendor.data.model

data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null
)

data class UploadPhotoResponse(
    val profile_photo: String
)
