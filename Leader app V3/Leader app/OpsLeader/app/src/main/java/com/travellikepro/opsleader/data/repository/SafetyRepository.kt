package com.travellikepro.opsleader.data.repository

import com.travellikepro.opsleader.data.api.*
import com.travellikepro.opsleader.data.firebase.FirebaseFirestoreService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafetyRepository @Inject constructor(
    private val apiService: ApiService,
    private val firestoreService: FirebaseFirestoreService? = null
) {
    suspend fun getSosAlerts(status: String? = null): Result<List<SosAlertDto>> {
        val fsResult = firestoreService?.getSosAlerts(status)
        if (fsResult != null && fsResult.isSuccess && fsResult.getOrNull()?.isNotEmpty() == true) {
            return fsResult
        }

        return try {
            val response = apiService.getSosAlerts(status)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to load SOS alerts"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun dispatchResponder(alertId: String, request: DispatchResponderRequest): Result<SosAlertDto> {
        val fsResult = firestoreService?.dispatchSosResponder(alertId, request)
        if (fsResult != null && fsResult.isSuccess) return fsResult

        return try {
            val response = apiService.dispatchSosResponder(alertId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to dispatch responder"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun resolveSosAlert(alertId: String, notes: String = "Resolved by Ops Leader"): Result<SosAlertDto> {
        val fsResult = firestoreService?.resolveSosAlert(alertId, mapOf("notes" to notes))
        if (fsResult != null && fsResult.isSuccess) return fsResult

        return try {
            val response = apiService.resolveSosAlert(alertId, mapOf("notes" to notes))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to resolve SOS alert"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun getIncidents(status: String? = null): Result<List<IncidentDto>> {
        val fsResult = firestoreService?.getIncidents(status)
        if (fsResult != null && fsResult.isSuccess && fsResult.getOrNull()?.isNotEmpty() == true) {
            return fsResult
        }

        return try {
            val response = apiService.getIncidents(status)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to load incidents"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun createIncident(request: CreateIncidentRequest): Result<IncidentDto> {
        val fsResult = firestoreService?.createIncident(request)
        if (fsResult != null && fsResult.isSuccess) return fsResult

        return try {
            val response = apiService.createIncident(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to create incident"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun updateIncidentStatus(incidentId: String, status: String, notes: String? = null): Result<IncidentDto> {
        return try {
            val body = mutableMapOf("status" to status)
            if (notes != null) body["resolution_notes"] = notes
            val response = apiService.updateIncidentStatus(incidentId, body)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.success(IncidentDto(id = incidentId, status = status))
            }
        } catch (e: Exception) {
            Result.success(IncidentDto(id = incidentId, status = status))
        }
    }

    suspend fun resolveIncident(incidentId: String, resolutionSummary: String): Result<IncidentDto> {
        return try {
            val body = mapOf("resolution_summary" to resolutionSummary, "resolved_by" to "Ops Leader")
            val response = apiService.resolveIncident(incidentId, body)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.success(IncidentDto(id = incidentId, status = "resolved", resolution_summary = resolutionSummary))
            }
        } catch (e: Exception) {
            Result.success(IncidentDto(id = incidentId, status = "resolved", resolution_summary = resolutionSummary))
        }
    }

    suspend fun getWeatherAlerts(): Result<List<Map<String, Any>>> {
        return try {
            val response = apiService.getWeatherAlerts()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }
}
