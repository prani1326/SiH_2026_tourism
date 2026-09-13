package com.travellikepro.opsleader.data.repository

import com.travellikepro.opsleader.data.api.ApiService
import com.travellikepro.opsleader.data.api.TouristDto
import com.travellikepro.opsleader.data.firebase.FirebaseFirestoreService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TouristRepository @Inject constructor(
    private val apiService: ApiService,
    private val firestoreService: FirebaseFirestoreService? = null
) {
    suspend fun getTourists(): Result<List<TouristDto>> {
        val fsResult = firestoreService?.getTourists()
        if (fsResult != null && fsResult.isSuccess && fsResult.getOrNull()?.isNotEmpty() == true) {
            return fsResult
        }

        return try {
            val response = apiService.getTourists()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to load tourists"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun getTouristById(touristId: String): Result<TouristDto> {
        val fsResult = firestoreService?.getTouristById(touristId)
        if (fsResult != null && fsResult.isSuccess) return fsResult

        return try {
            val response = apiService.getTouristById(touristId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Tourist not found"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }
}
