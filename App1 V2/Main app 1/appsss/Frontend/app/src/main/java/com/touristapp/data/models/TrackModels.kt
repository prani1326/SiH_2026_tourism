package com.touristapp.data.models

import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TripLeaderDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("phone") val phone: String = "",
    @SerialName("photo_url") val photoUrl: String = "",
    @SerialName("rating") val rating: Double = 4.9,
    @SerialName("role") val role: String = "Certified Trip Leader",
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("is_online") val isOnline: Boolean = true,
    @SerialName("last_seen") val lastSeen: String = "Active now"
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): TripLeaderDto {
            val data = doc.data ?: emptyMap()
            return TripLeaderDto(
                id = doc.id,
                name = data["name"] as? String ?: "Trip Leader",
                phone = data["phone"] as? String ?: "+91 98765 43210",
                photoUrl = data["photo_url"] as? String ?: "",
                rating = (data["rating"] as? Number)?.toDouble() ?: 4.9,
                role = data["role"] as? String ?: "Certified Trip Leader",
                latitude = (data["latitude"] as? Number)?.toDouble(),
                longitude = (data["longitude"] as? Number)?.toDouble(),
                isOnline = (data["is_online"] as? Boolean) ?: true,
                lastSeen = data["last_seen"] as? String ?: "Active now"
            )
        }
    }
}

@Serializable
data class TripTransportDto(
    @SerialName("id") val id: String = "",
    @SerialName("vehicle_type") val vehicleType: String = "Tourist AC Bus / Cab",
    @SerialName("vehicle_number") val vehicleNumber: String = "DL 01 AB 1234",
    @SerialName("driver_name") val driverName: String = "Rajesh Kumar",
    @SerialName("driver_phone") val driverPhone: String = "+91 91234 56789",
    @SerialName("status") val status: String = "On the way", // "Assigned", "On the way", "Arrived", "In transit", "Completed"
    @SerialName("pickup_point") val pickupPoint: String = "Hotel Entrance / Designated Pickup",
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("eta_minutes") val etaMinutes: Int = 12
) {
    companion object {
        fun fromFirestore(doc: DocumentSnapshot): TripTransportDto {
            val data = doc.data ?: emptyMap()
            return TripTransportDto(
                id = doc.id,
                vehicleType = data["vehicle_type"] as? String ?: "Tourist AC Bus / Cab",
                vehicleNumber = data["vehicle_number"] as? String ?: "DL 01 AB 1234",
                driverName = data["driver_name"] as? String ?: "Rajesh Kumar",
                driverPhone = data["driver_phone"] as? String ?: "+91 91234 56789",
                status = data["status"] as? String ?: "On the way",
                pickupPoint = data["pickup_point"] as? String ?: "Designated Pickup Point",
                latitude = (data["latitude"] as? Number)?.toDouble(),
                longitude = (data["longitude"] as? Number)?.toDouble(),
                etaMinutes = (data["eta_minutes"] as? Number)?.toInt() ?: 12
            )
        }
    }
}
