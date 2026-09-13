package com.touristapp.data.models

import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaceModel(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("latitude") val latitude: Double = 0.0,
    @SerialName("longitude") val longitude: Double = 0.0,
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("category") val category: String = "attraction", // "hotel", "food", "activity", "attraction", "transport", "meeting"
    @SerialName("rating") val rating: Double = 4.5,
    @SerialName("price") val price: String? = null,
    @SerialName("destination_id") val destinationId: String = "",
    @SerialName("destination_name") val destinationName: String = "",
    @SerialName("availability") val availability: String? = "Open Daily 9:00 AM - 6:00 PM",
    @SerialName("address") val address: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "latitude" to latitude,
            "longitude" to longitude,
            "image_url" to imageUrl,
            "category" to category,
            "rating" to rating,
            "price" to price,
            "destination_id" to destinationId,
            "destination_name" to destinationName,
            "availability" to availability,
            "address" to address,
            "is_active" to isActive,
            "created_at" to createdAt,
            "updated_at" to updatedAt
        )
    }

    companion object {
        fun fromFirestore(doc: DocumentSnapshot): PlaceModel {
            val data = doc.data ?: emptyMap()
            return PlaceModel(
                id = doc.id,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0,
                imageUrl = (data["image_url"] as? String) ?: (data["cover_image"] as? String) ?: "",
                category = data["category"] as? String ?: "attraction",
                rating = (data["rating"] as? Number)?.toDouble() ?: 4.5,
                price = data["price"] as? String,
                destinationId = data["destination_id"] as? String ?: "",
                destinationName = data["destination_name"] as? String ?: "",
                availability = data["availability"] as? String ?: "Open Daily 9:00 AM - 6:00 PM",
                address = data["address"] as? String,
                isActive = (data["is_active"] as? Boolean) ?: true,
                createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                updatedAt = (data["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
