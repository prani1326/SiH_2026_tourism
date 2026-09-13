package com.travellikepro.travelvendor.data.model

data class Listing(
    val id: Int = 0,
    val vendor_id: Int = 0,
    val title: String = "",
    val description: String? = null,
    val destination: String = "",
    val price: Double = 0.0,
    val duration: String = "",
    val max_travellers: Int = 10,
    val status: String = "active",
    val created_at: String? = null,
    val updated_at: String? = null,
    val stats: ListingStats? = null
)

data class ListingStats(
    val total_bookings: Int = 0,
    val completed_bookings: Int = 0,
    val accepted_bookings: Int = 0
)

data class ListingsResponseData(
    val listings: List<Listing> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)

data class CreateListingRequest(
    val title: String,
    val description: String? = null,
    val destination: String,
    val price: Double,
    val duration: String,
    val max_travellers: Int = 10,
    val status: String = "active"
)
