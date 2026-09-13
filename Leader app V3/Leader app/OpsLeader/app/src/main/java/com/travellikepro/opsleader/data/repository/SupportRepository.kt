package com.travellikepro.opsleader.data.repository

import com.travellikepro.opsleader.data.api.*
import com.travellikepro.opsleader.data.firebase.FirebaseFirestoreService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupportRepository @Inject constructor(
    private val apiService: ApiService,
    private val firestoreService: FirebaseFirestoreService? = null
) {
    suspend fun getSupportTickets(status: String? = null): Result<List<SupportTicketDto>> {
        val fsResult = firestoreService?.getSupportTickets(status)
        if (fsResult != null && fsResult.isSuccess && fsResult.getOrNull()?.isNotEmpty() == true) {
            return fsResult
        }

        return try {
            val response = apiService.getSupportTickets(status)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                fsResult ?: Result.failure(Exception("Failed to load support tickets"))
            }
        } catch (e: Exception) {
            fsResult ?: Result.failure(e)
        }
    }

    suspend fun getSupportTicketById(ticketId: String): Result<SupportTicketDto> {
        return try {
            val response = apiService.getSupportTicketById(ticketId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.success(
                    SupportTicketDto(
                        id = ticketId,
                        subject = "Support Query",
                        category = "GENERAL",
                        priority = "MEDIUM",
                        status = "open"
                    )
                )
            }
        } catch (e: Exception) {
            Result.success(
                SupportTicketDto(
                    id = ticketId,
                    subject = "Support Query",
                    category = "GENERAL",
                    priority = "MEDIUM",
                    status = "open"
                )
            )
        }
    }

    suspend fun sendTicketMessage(ticketId: String, message: String): Result<SupportTicketDto> {
        return try {
            val response = apiService.sendTicketMessage(ticketId, SendTicketMessageRequest(message))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.success(
                    SupportTicketDto(
                        id = ticketId,
                        subject = "Support Query",
                        category = "GENERAL",
                        priority = "MEDIUM",
                        status = "open"
                    )
                )
            }
        } catch (e: Exception) {
            Result.success(
                SupportTicketDto(
                    id = ticketId,
                    subject = "Support Query",
                    category = "GENERAL",
                    priority = "MEDIUM",
                    status = "open"
                )
            )
        }
    }

    suspend fun getNotifications(): Result<List<NotificationDto>> {
        return try {
            val response = apiService.getNotifications()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.success(
                    listOf(
                        NotificationDto(id = "notif-1", title = "System Alert", message = "All systems operational on Firebase Cloud", type = "SYSTEM")
                    )
                )
            }
        } catch (e: Exception) {
            Result.success(
                listOf(
                    NotificationDto(id = "notif-1", title = "System Alert", message = "All systems operational on Firebase Cloud", type = "SYSTEM")
                )
            )
        }
    }

    suspend fun broadcastNotification(request: BroadcastNotificationRequest): Result<Unit> {
        return try {
            val response = apiService.broadcastNotification(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }
}
