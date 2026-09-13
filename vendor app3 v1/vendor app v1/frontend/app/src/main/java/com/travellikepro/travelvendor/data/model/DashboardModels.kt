package com.travellikepro.travelvendor.data.model

data class DashboardData(
    val vendor: Vendor? = null,
    val kyc_status: String? = null,
    val today_trips: Int = 0,
    val total_bookings: Int = 0,
    val pending_requests: Int = 0,
    val accepted_bookings: Int = 0,
    val upcoming_trips: Int = 0,
    val active_trip: ActiveTripInfo? = null,
    val wallet_balance: Double = 0.0,
    val total_earnings: Double = 0.0
)

data class ActiveTripInfo(
    val id: Int,
    val booking_id: Int,
    val listing_title: String? = null,
    val listing_destination: String? = null,
    val traveller_name: String? = null,
    val traveller_mobile: String? = null,
    val amount: Double = 0.0,
    val traveller_count: Int = 1,
    val status: String = "active"
)
