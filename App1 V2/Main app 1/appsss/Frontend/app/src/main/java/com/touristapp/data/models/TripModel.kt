package com.touristapp.data.models

import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class TripModel(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val destinationName: String = "",
    val destinationId: String? = null,
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "planning", // "planning", "active", "completed"
    val daysCount: Int = 3,
    val travelersCount: Int = 1,
    val budgetTotal: Double = 0.0,
    val budgetSpent: Double = 0.0,
    val style: String = "Solo",
    val interests: List<String> = emptyList(),
    val itineraryJson: String = "",
    val leaderId: String? = null,
    val transportId: String? = null,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val approvalStatus: String = "approved"
) {
    fun toJsonObject(): JsonObject {
        return buildJsonObject {
            put("id", id)
            put("user_id", userId)
            put("title", title)
            put("destination_name", destinationName)
            put("destination_id", destinationId ?: "")
            put("start_date", startDate)
            put("end_date", endDate)
            put("status", status)
            put("day_count", daysCount)
            put("traveler_count", travelersCount)
            put("budget_total", budgetTotal)
            put("budget_spent", budgetSpent)
            put("style", style)
            put("interests", buildJsonArray { interests.forEach { add(JsonPrimitive(it)) } })
            put("itinerary_json", itineraryJson)
            leaderId?.let { put("leader_id", it) }
            transportId?.let { put("transport_id", it) }
            destinationLatitude?.let { put("destination_latitude", it) }
            destinationLongitude?.let { put("destination_longitude", it) }
            put("approval_status", approvalStatus)
        }
    }

    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "user_id" to userId,
            "title" to title,
            "destination_name" to destinationName,
            "destination_id" to destinationId,
            "start_date" to startDate,
            "end_date" to endDate,
            "status" to status,
            "day_count" to daysCount,
            "traveler_count" to travelersCount,
            "budget_total" to budgetTotal,
            "budget_spent" to budgetSpent,
            "style" to style,
            "interests" to interests,
            "itinerary_json" to itineraryJson,
            "leader_id" to leaderId,
            "transport_id" to transportId,
            "destination_latitude" to destinationLatitude,
            "destination_longitude" to destinationLongitude,
            "approval_status" to approvalStatus,
            "updated_at" to com.google.firebase.Timestamp.now()
        )
    }

    companion object {
        fun fromFirestore(doc: DocumentSnapshot): TripModel {
            val data = doc.data ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            return TripModel(
                id = doc.id,
                userId = data["user_id"] as? String ?: "",
                title = data["title"] as? String ?: "My Trip",
                destinationName = data["destination_name"] as? String ?: "Destination",
                destinationId = data["destination_id"] as? String,
                startDate = data["start_date"] as? String ?: "",
                endDate = data["end_date"] as? String ?: "",
                status = data["status"] as? String ?: "planning",
                daysCount = (data["day_count"] as? Number)?.toInt() ?: 3,
                travelersCount = (data["traveler_count"] as? Number)?.toInt() ?: 1,
                budgetTotal = (data["budget_total"] as? Number)?.toDouble() ?: 0.0,
                budgetSpent = (data["budget_spent"] as? Number)?.toDouble() ?: 0.0,
                style = data["style"] as? String ?: "Solo",
                interests = (data["interests"] as? List<String>) ?: emptyList(),
                itineraryJson = data["itinerary_json"] as? String ?: "",
                leaderId = data["leader_id"] as? String,
                transportId = data["transport_id"] as? String,
                destinationLatitude = (data["destination_latitude"] as? Number)?.toDouble(),
                destinationLongitude = (data["destination_longitude"] as? Number)?.toDouble(),
                approvalStatus = data["approval_status"] as? String ?: "approved"
            )
        }
    }
}
