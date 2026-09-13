package com.travellikepro.travelvendor.data.repository

import android.content.Context
import com.travellikepro.travelvendor.data.api.RetrofitClient
import com.travellikepro.travelvendor.data.model.KycSubmitRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class KycRepository(context: Context) {
    private val api = RetrofitClient.getApi(context)

    suspend fun getKyc() = api.getKyc()

    suspend fun getKycStatus() = api.getKycStatus()

    suspend fun submitKyc(request: KycSubmitRequest) = api.submitKyc(request)

    suspend fun uploadDocument(file: File, docType: String) {
        val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("document", file.name, requestFile)
        val docTypeBody = docType.toRequestBody("text/plain".toMediaTypeOrNull())
        api.uploadKycDocument(body, docTypeBody)
    }
}
