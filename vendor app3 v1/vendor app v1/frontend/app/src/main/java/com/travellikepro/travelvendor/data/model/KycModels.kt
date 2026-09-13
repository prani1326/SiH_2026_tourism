package com.travellikepro.travelvendor.data.model

data class KycResponseData(
    val kyc_status: String = "pending",
    val rejection_reason: String? = null,
    val verification_remarks: String? = null,
    val verified_at: String? = null,
    val details: KycDetails? = null,
    val documents: List<KycDocument> = emptyList()
)

data class KycDetails(
    val id: Int? = null,
    val vendor_id: Int? = null,
    val full_name: String? = null,
    val mobile: String? = null,
    val email: String? = null,
    val dob: String? = null,
    val business_name: String? = null,
    val business_type: String? = null,
    val pan_number: String? = null,
    val aadhaar_number: String? = null,
    val gst_number: String? = null,
    val tourism_license_no: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String? = null
)

data class KycDocument(
    val id: Int = 0,
    val vendor_id: Int = 0,
    val doc_type: String = "",
    val file_path: String = "",
    val file_name: String = "",
    val file_size: Long = 0,
    val mime_type: String = "",
    val uploaded_at: String = ""
)

data class KycSubmitRequest(
    val full_name: String,
    val mobile: String? = null,
    val email: String? = null,
    val dob: String? = null,
    val business_name: String,
    val business_type: String,
    val pan_number: String? = null,
    val aadhaar_number: String? = null,
    val gst_number: String? = null,
    val tourism_license_no: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)
