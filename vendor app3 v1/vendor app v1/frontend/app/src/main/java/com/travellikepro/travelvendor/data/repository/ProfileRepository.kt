package com.travellikepro.travelvendor.data.repository

import android.content.Context
import com.travellikepro.travelvendor.data.api.RetrofitClient
import com.travellikepro.travelvendor.data.model.UpdateProfileRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ProfileRepository(context: Context) {
    private val api = RetrofitClient.getApi(context)

    suspend fun getProfile() = api.getProfile()

    suspend fun updateProfile(name: String, email: String) =
        api.updateProfile(UpdateProfileRequest(name = name, email = email))

    suspend fun uploadProfilePhoto(file: File) {
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("profile_photo", file.name, requestFile)
        api.uploadProfilePhoto(body)
    }
}
