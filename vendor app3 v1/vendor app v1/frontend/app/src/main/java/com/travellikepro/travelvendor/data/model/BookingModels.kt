package com.travellikepro.travelvendor.data.model

data class Booking(
    val id: Int = 0,
    val listing_id: Int = 0,
    val vendor_id: Int = 0,
    val traveller_id: Int = 0,
    val booking_date: String = "",
    val traveller_count: Int = 1,
    val amount: Double = 0.0,
    val status: String = "pending",
    val created_at: String? = null,
    val updated_at: String? = null,
    val listing_title: String? = null,
    val listing_description: String? = null,
    val listing_destination: String? = null,
    val listing_duration: String? = null,
    val traveller_name: String? = null,
    val traveller_email: String? = null,
    val traveller_mobile: String? = null,
    val traveller_photo: String? = null,
    val trip_id: Int? = null,
    val trip_status: String? = null
)

data class BookingCounts(
    val total: Int = 0,
    val pending: Int = 0,
    val accepted: Int = 0,
    val rejected: Int = 0,
    val completed: Int = 0
)

data class BookingsResponseData(
    val bookings: List<Booking> = emptyList(),
    val counts: BookingCounts = BookingCounts(),
    val page: Int = 1,
    val limit: Int = 20
)

data class RejectBookingRequest(
    val reason: String? = null
)
