package com.touristapp.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class KycStatus(val displayName: String) {
    NOT_STARTED("Not Started"),
    SKIPPED("Not Completed"),
    PENDING("Verification Pending"),
    VERIFIED("✓ KYC Verified"),
    FAILED("Verification Failed")
}

@Serializable
data class AddressData(
    val address_line1: String = "",
    val address_line2: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "India",
    val postal_code: String = ""
)

@Serializable
data class KycData(
    val status: String = "NOT_STARTED",
    val document_type: String = "Passport",
    val document_number: String = "",
    val verification_provider: String = "Government Identity Services",
    val verification_reference: String = "",
    val submitted_at: String? = null,
    val verified_at: String? = null
)

@Serializable
data class TripEligibilityResponse(
    val eligible: Boolean = false,
    val profile_complete: Boolean = false,
    val kyc_verified: Boolean = false,
    val profile_completion: Float = 0f,
    val kyc_status: String = "NOT_STARTED",
    val missing_requirements: List<String> = emptyList(),
    val message: String = ""
)

@Serializable
data class ProfileStepPayload(
    val step: Int,
    val data: Map<String, String>
)

@Serializable
data class KycSubmitPayload(
    val document_type: String = "Passport",
    val document_number: String = "",
    val document_image_url: String? = null,
    val expiry_date: String? = null
)

@Serializable
data class KycVerifyPayload(
    val document_type: String = "Passport",
    val document_number: String = "P12345678"
)

@Serializable
data class ProfileKycActionResponse(
    val success: Boolean = true,
    val step: Int? = null,
    val profile_completion: Float? = null,
    val profile_status: String? = null,
    val kyc_status: String? = null,
    val kyc_verified: Boolean? = null,
    val verification_reference: String? = null,
    val message: String? = null
)
