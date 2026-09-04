package com.travellikepro.opsleader.data.model.triprequest

import java.util.UUID

enum class TripRequestStatus {
    NEW,
    UNDER_REVIEW,
    AWAITING_INFO,
    APPROVED, // Converts into a Booking/Active Trip
    REJECTED,
    EXPIRED
}

enum class SourceChannel {
    APP,
    WEB,
    AGENT,
    CALL_CENTER
}

data class TripRequest(
    val id: String = UUID.randomUUID().toString(),
    val requesterName: String,
    val requesterContact: String,
    val destination: String,
    val preferredDates: String,
    val groupSize: Int,
    val budgetRange: String,
    val specialRequirements: String,
    val sourceChannel: SourceChannel,
    val submittedTimestamp: Long, // Epoch millis
    val slaDeadlineTimestamp: Long, // Epoch millis
    val status: TripRequestStatus = TripRequestStatus.NEW,
    val assignedReviewer: String? = null,
    val rejectionReason: String? = null,
    val timeline: List<TripRequestActivity> = emptyList()
)

data class TripRequestActivity(
    val timestamp: Long,
    val action: String,
    val performedBy: String
)
