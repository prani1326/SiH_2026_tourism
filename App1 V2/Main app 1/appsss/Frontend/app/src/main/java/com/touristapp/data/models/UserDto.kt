package com.touristapp.data.models

import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String = "",
    val email: String? = null,
    val phone: String? = null,
    val full_name: String = "",
    val is_verified: Boolean = false,
    val email_verified: Boolean = true,
    val phone_verified: Boolean = true,
    val role: String = "tourist",
    val profile_status: String = "PROFILE_INCOMPLETE",
    val profile_completion: Float = 40f,
    val kyc_status: String = "NOT_STARTED",
    val kyc_verified: Boolean = false,
    val onboarding_step: Int = 1,
    val profile: UserProfileDto? = null,
    val address: AddressData? = null,
    val kyc: KycData? = null,
    val preferences: UserPreferencesDto? = null,
    val emergency_contacts: List<EmergencyContactDto> = emptyList()
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "email" to email,
            "phone" to phone,
            "full_name" to full_name,
            "is_verified" to is_verified,
            "profile_status" to profile_status,
            "profile_completion" to profile_completion,
            "kyc_status" to kyc_status,
            "kyc_verified" to kyc_verified,
            "onboarding_step" to onboarding_step,
            "role" to role,
            "profile" to profile?.let {
                mapOf(
                    "avatar_url" to it.avatar_url,
                    "bio" to it.bio,
                    "first_name" to it.first_name,
                    "last_name" to it.last_name,
                    "date_of_birth" to it.date_of_birth,
                    "gender" to it.gender,
                    "nationality" to it.nationality,
                    "language" to it.language,
                    "preferred_language" to it.preferred_language,
                    "currency" to it.currency,
                    "walking_tolerance" to it.walking_tolerance,
                    "pace" to it.pace
                )
            },
            "address" to address?.let {
                mapOf(
                    "address_line1" to it.address_line1,
                    "address_line2" to it.address_line2,
                    "city" to it.city,
                    "state" to it.state,
                    "country" to it.country,
                    "postal_code" to it.postal_code
                )
            },
            "kyc" to kyc?.let {
                mapOf(
                    "status" to it.status,
                    "document_type" to it.document_type,
                    "document_number" to it.document_number,
                    "verification_provider" to it.verification_provider,
                    "verification_reference" to it.verification_reference,
                    "submitted_at" to it.submitted_at,
                    "verified_at" to it.verified_at
                )
            },
            "preferences" to preferences?.let {
                mapOf(
                    "preferred_destinations" to it.preferred_destinations,
                    "budget_range" to it.budget_range,
                    "travel_styles" to it.travel_styles,
                    "interests" to it.interests,
                    "dietary_preferences" to it.dietary_preferences
                )
            },
            "updated_at" to com.google.firebase.Timestamp.now()
        )
    }

    companion object {
        fun fromFirestore(doc: DocumentSnapshot): UserDto {
            val data = doc.data ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val profileMap = data["profile"] as? Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val addressMap = data["address"] as? Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val kycMap = data["kyc"] as? Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val prefsMap = data["preferences"] as? Map<String, Any?>

            return UserDto(
                id = doc.id,
                email = data["email"] as? String,
                phone = data["phone"] as? String,
                full_name = data["full_name"] as? String ?: "Traveler",
                is_verified = (data["is_verified"] as? Boolean) ?: false,
                email_verified = (data["email_verified"] as? Boolean) ?: true,
                phone_verified = (data["phone_verified"] as? Boolean) ?: true,
                role = data["role"] as? String ?: "tourist",
                profile_status = data["profile_status"] as? String ?: "PROFILE_INCOMPLETE",
                profile_completion = (data["profile_completion"] as? Number)?.toFloat() ?: 40f,
                kyc_status = data["kyc_status"] as? String ?: "NOT_STARTED",
                kyc_verified = (data["kyc_verified"] as? Boolean) ?: false,
                onboarding_step = (data["onboarding_step"] as? Number)?.toInt() ?: 1,
                profile = profileMap?.let {
                    UserProfileDto(
                        avatar_url = it["avatar_url"] as? String,
                        bio = it["bio"] as? String,
                        first_name = it["first_name"] as? String,
                        last_name = it["last_name"] as? String,
                        date_of_birth = it["date_of_birth"] as? String,
                        gender = it["gender"] as? String ?: "Prefer not to say",
                        nationality = it["nationality"] as? String ?: "Indian",
                        language = it["language"] as? String ?: "English",
                        preferred_language = it["preferred_language"] as? String ?: "English",
                        currency = it["currency"] as? String ?: "INR",
                        walking_tolerance = it["walking_tolerance"] as? String ?: "Moderate",
                        pace = it["pace"] as? String ?: "Moderate"
                    )
                },
                address = addressMap?.let {
                    AddressData(
                        address_line1 = it["address_line1"] as? String ?: "",
                        address_line2 = it["address_line2"] as? String ?: "",
                        city = it["city"] as? String ?: "",
                        state = it["state"] as? String ?: "",
                        country = it["country"] as? String ?: "India",
                        postal_code = it["postal_code"] as? String ?: ""
                    )
                },
                kyc = kycMap?.let {
                    KycData(
                        status = it["status"] as? String ?: "NOT_STARTED",
                        document_type = it["document_type"] as? String ?: "Passport",
                        document_number = it["document_number"] as? String ?: "",
                        verification_provider = it["verification_provider"] as? String ?: "Government Identity Services",
                        verification_reference = it["verification_reference"] as? String ?: "",
                        submitted_at = it["submitted_at"] as? String,
                        verified_at = it["verified_at"] as? String
                    )
                },
                preferences = prefsMap?.let {
                    @Suppress("UNCHECKED_CAST")
                    UserPreferencesDto(
                        preferred_destinations = (it["preferred_destinations"] as? List<String>) ?: emptyList(),
                        budget_range = it["budget_range"] as? String,
                        travel_styles = (it["travel_styles"] as? List<String>) ?: emptyList(),
                        interests = (it["interests"] as? List<String>) ?: emptyList(),
                        dietary_preferences = (it["dietary_preferences"] as? List<String>) ?: emptyList()
                    )
                }
            )
        }
    }
}

@Serializable
data class UserProfileDto(
    val avatar_url: String? = null,
    val bio: String? = null,
    val first_name: String? = null,
    val last_name: String? = null,
    val date_of_birth: String? = null,
    val gender: String? = "Prefer not to say",
    val nationality: String? = "Indian",
    val language: String? = "English",
    val preferred_language: String? = "English",
    val currency: String? = "INR",
    val walking_tolerance: String? = "Moderate",
    val pace: String? = "Moderate"
)

@Serializable
data class UserPreferencesDto(
    val preferred_destinations: List<String> = emptyList(),
    val budget_range: String? = null,
    val travel_styles: List<String> = emptyList(),
    val interests: List<String> = emptyList(),
    val dietary_preferences: List<String> = emptyList()
)

@Serializable
data class EmergencyContactDto(
    val id: String = "",
    val contact_name: String = "",
    val relationship_type: String = "Family",
    val phone_number: String = "",
    val email: String? = null,
    val is_primary: Boolean = false
)
