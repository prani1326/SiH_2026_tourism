package com.travellikepro.opsleader.data.repository

import com.travellikepro.opsleader.data.api.*
import com.travellikepro.opsleader.data.firebase.FirebaseFirestoreService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OperationsRepository @Inject constructor(
    private val apiService: ApiService,
    private val firestoreService: FirebaseFirestoreService? = null
) {
    suspend fun getDashboardKpis(): Result<DashboardKpiDto> {
        val fsResult = firestoreService?.getDashboardKpis()
        if (fsResult != null && fsResult.isSuccess) return fsResult

        return try {
            val response = apiService.getDashboardKpis()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to load KPIs"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun getDashboardStats(): Result<DashboardStatsDto> {
        val fsResult = firestoreService?.getDashboardStats()
        if (fsResult != null && fsResult.isSuccess) return fsResult

        return try {
            val response = apiService.getDashboardStats()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to load stats"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun getTrips(status: String? = null): Result<List<TripDto>> {
        val fsResult = firestoreService?.getTrips(status)
        if (fsResult != null && fsResult.isSuccess && fsResult.getOrNull()?.isNotEmpty() == true) {
            return fsResult
        }

        return try {
            val response = apiService.getTrips(status)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to load trips"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun getTripById(tripId: String): Result<TripDto> {
        val fsResult = firestoreService?.getTripById(tripId)
        if (fsResult != null && fsResult.isSuccess) return fsResult

        return try {
            val response = apiService.getTripById(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Trip not found"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun assignTrip(tripId: String, request: AssignTripRequest): Result<TripDto> {
        val fsResult = firestoreService?.assignTrip(tripId, request)
        if (fsResult != null && fsResult.isSuccess) return fsResult

        return try {
            val response = apiService.assignTrip(tripId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to assign trip"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun addTripNote(tripId: String, content: String): Result<Unit> {
        val fsResult = firestoreService?.addTripNote(tripId, content)
        if (fsResult != null && fsResult.isSuccess) return fsResult

        return try {
            val response = apiService.addTripNote(tripId, AddNoteRequest(content))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                fsResult ?: Result.failure(Exception("Failed to add note"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun getBookings(status: String? = null): Result<List<BookingDto>> {
        val fsResult = firestoreService?.getBookings(status)
        if (fsResult != null && fsResult.isSuccess && fsResult.getOrNull()?.isNotEmpty() == true) {
            return fsResult
        }

        return try {
            val response = apiService.getBookings(status)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to load bookings"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun getPartners(serviceType: String? = null): Result<List<VendorDto>> {
        val fsResult = firestoreService?.getPartners(serviceType)
        if (fsResult != null && fsResult.isSuccess && fsResult.getOrNull()?.isNotEmpty() == true) {
            return fsResult
        }

        return try {
            val response = apiService.getPartners(serviceType)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to load partners"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun getTeam(): Result<List<TeamMemberDto>> {
        return try {
            val response = apiService.getTeam()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.success(
                    listOf(
                        TeamMemberDto("usr-ops-01", "Priya Patel", "priya.ops@leaderops.internal", "Ops Leader", "active", 3),
                        TeamMemberDto("usr-admin-01", "Admin Command", "admin@leaderops.internal", "Super Admin", "active", 5)
                    )
                )
            }
        } catch (e: Exception) {
            Result.success(
                listOf(
                    TeamMemberDto("usr-ops-01", "Priya Patel", "priya.ops@leaderops.internal", "Ops Leader", "active", 3),
                    TeamMemberDto("usr-admin-01", "Admin Command", "admin@leaderops.internal", "Super Admin", "active", 5)
                )
            )
        }
    }

    suspend fun getChartData(type: String, days: Int = 7): Result<List<Map<String, Any>>> {
        return try {
            val response = apiService.getChartData(type, days)
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
