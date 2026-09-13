package com.touristapp.data.models

import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class BookingModel(
    val id: String = "",
    val userId: String = "",
    val tripId: String? = null,
    val destinationId: String = "",
    val destinationName: String = "",
    val itemType: String = "hotel",
    val itemId: String = "",
    val itemTitle: String = "",
    val bookingReference: String = "",
    val status: String = "confirmed", // "pending", "confirmed", "cancelled"
    val bookingDate: String = "",
    val travelDate: String = "",
    val checkInDate: String = "",
    val checkOutDate: String = "",
    val numberOfGuests: Int = 1,
    val guestCount: Int = 1,
    val totalAmount: Double = 0.0,
    val paymentStatus: String = "paid",
    val qrCodeBase64: String? = null
) {
    fun toJsonObject(): JsonObject {
        return buildJsonObject {
            put("id", id)
            put("user_id", userId)
            put("trip_id", tripId ?: "")
            put("destination_id", destinationId)
            put("destination_name", destinationName.ifBlank { itemTitle })
            put("item_type", itemType)
            put("item_id", itemId)
            put("item_title", itemTitle.ifBlank { destinationName })
            put("booking_reference", bookingReference)
            put("status", status)
            put("booking_date", bookingDate)
            put("travel_date", travelDate.ifBlank { checkInDate })
            put("check_in_date", checkInDate.ifBlank { travelDate })
            put("check_out_date", checkOutDate)
            put("number_of_guests", numberOfGuests)
            put("guest_count", guestCount)
            put("total_amount", totalAmount)
            put("payment_status", paymentStatus)
            put("qr_code_base64", qrCodeBase64 ?: "")
        }
    }

    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "user_id" to userId,
            "trip_id" to tripId,
            "destination_id" to destinationId,
            "destination_name" to destinationName.ifBlank { itemTitle },
            "item_type" to itemType,
            "item_id" to itemId,
            "item_title" to itemTitle.ifBlank { destinationName },
            "booking_reference" to bookingReference,
            "status" to status,
            "booking_date" to bookingDate,
            "travel_date" to travelDate.ifBlank { checkInDate },
            "check_in_date" to checkInDate.ifBlank { travelDate },
            "check_out_date" to checkOutDate,
            "number_of_guests" to numberOfGuests,
            "guest_count" to guestCount,
            "total_amount" to totalAmount,
            "payment_status" to paymentStatus,
            "qr_code_base64" to qrCodeBase64,
            "created_at" to com.google.firebase.Timestamp.now()
        )
    }

    companion object {
        fun fromFirestore(doc: DocumentSnapshot): BookingModel {
            val data = doc.data ?: emptyMap()
            val guests = (data["guest_count"] as? Number)?.toInt()
                ?: (data["number_of_guests"] as? Number)?.toInt() ?: 1
            val title = (data["item_title"] as? String)
                ?: (data["destination_name"] as? String) ?: "Booking"

            return BookingModel(
                id = doc.id,
                userId = data["user_id"] as? String ?: "",
                tripId = data["trip_id"] as? String,
                destinationId = data["destination_id"] as? String ?: "",
                destinationName = title,
                itemType = data["item_type"] as? String ?: "hotel",
                itemId = data["item_id"] as? String ?: "",
                itemTitle = title,
                bookingReference = data["booking_reference"] as? String ?: "",
                status = data["status"] as? String ?: "confirmed",
                bookingDate = data["booking_date"] as? String ?: "",
                travelDate = data["travel_date"] as? String ?: "",
                checkInDate = data["check_in_date"] as? String ?: (data["travel_date"] as? String ?: ""),
                checkOutDate = data["check_out_date"] as? String ?: "",
                numberOfGuests = guests,
                guestCount = guests,
                totalAmount = (data["total_amount"] as? Number)?.toDouble() ?: 0.0,
                paymentStatus = data["payment_status"] as? String
                    ?: (if ((data["status"] as? String)?.equals("confirmed", ignoreCase = true) == true) "paid" else "pending"),
                qrCodeBase64 = (data["voucher_qr_data"] as? String) ?: (data["qr_code_base64"] as? String)
            )
        }
    }
}
