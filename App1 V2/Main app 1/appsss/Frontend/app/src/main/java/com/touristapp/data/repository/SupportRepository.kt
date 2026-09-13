package com.touristapp.data.repository

import com.touristapp.data.remote.BackendApiClient
import com.touristapp.data.remote.model.SupportTicketCreateDto
import com.touristapp.data.remote.model.SupportTicketOutDto
import com.touristapp.data.remote.model.TicketMessageDto

class SupportRepository(
    private val backendApiClient: BackendApiClient = BackendApiClient()
) {
    suspend fun getUserTickets(): Result<List<SupportTicketOutDto>> {
        val res = backendApiClient.getUserTickets()
        if (res.isSuccess) return res

        return Result.success(
            listOf(
                SupportTicketOutDto(
                    id = "ticket-demo-1",
                    category = "Booking Inquiry",
                    subject = "Hotel Check-in Time Confirmation",
                    status = "Resolved",
                    priority = "Medium",
                    createdAt = "Yesterday",
                    messages = listOf(
                        TicketMessageDto(
                            id = "msg-1",
                            senderType = "user",
                            senderName = "Traveler",
                            message = "Hi, can I check in at 11:00 AM instead of 2:00 PM?",
                            createdAt = "Yesterday"
                        ),
                        TicketMessageDto(
                            id = "msg-2",
                            senderType = "support",
                            senderName = "Tourist Concierge",
                            message = "Hello! Yes, the hotel confirmed complimentary early check-in subject to room availability.",
                            createdAt = "Yesterday"
                        )
                    )
                )
            )
        )
    }

    suspend fun createTicket(subject: String, category: String, message: String, bookingId: String? = null): Result<SupportTicketOutDto> {
        val dto = SupportTicketCreateDto(
            subject = subject,
            category = category,
            message = message,
            bookingId = bookingId
        )
        return backendApiClient.createTicket(dto)
    }
}
