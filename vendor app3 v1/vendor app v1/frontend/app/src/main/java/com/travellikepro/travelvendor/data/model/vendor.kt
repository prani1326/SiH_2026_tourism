package com.travellikepro.travelvendor.data.model

data class Vendor(
    val id: Int? = null,
    val name: String? = null,
    val email: String? = null,
    val mobile: String? = null,
    val profile_photo: String? = null,
    val status: String? = null,
    val kyc_status: String? = null,
    val rejection_reason: String? = null,
    val verification_remarks: String? = null,
    val verified_at: String? = null
)