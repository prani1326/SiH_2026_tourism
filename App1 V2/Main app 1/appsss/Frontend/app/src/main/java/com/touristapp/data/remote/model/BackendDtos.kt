package com.touristapp.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- AI Trip Planner DTOs ---

@Serializable
data class BudgetBreakdownDto(
    val hotel: Double = 0.0,
    val food: Double = 0.0,
    val transport: Double = 0.0,
    val activities: Double = 0.0,
    val miscellaneous: Double = 0.0
)

@Serializable
data class AITripPlanRequestDto(
    val destination: String,
    @SerialName("destination_id") val destinationId: String? = null,
    @SerialName("duration_days") val durationDays: Int = 3,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("traveler_count") val travelerCount: Int = 2,
    val budget: Double = 25000.0,
    val currency: String = "INR",
    @SerialName("travel_style") val travelStyle: String = "Couple",
    val interests: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    @SerialName("food_preference") val foodPreference: String = "Local Food",
    @SerialName("hotel_preference") val hotelPreference: String = "3 Star",
    @SerialName("transport_preference") val transportPreference: String = "Cab",
    @SerialName("travel_pace") val travelPace: String = "Balanced",
    @SerialName("walking_tolerance") val walkingTolerance: String = "Moderate",
    val pace: String = "Balanced"
)

@Serializable
data class GeneratedActivityDto(
    val id: String? = null,
    @SerialName("time_slot") val timeSlot: String = "Morning",
    val title: String = "",
    val description: String? = null,
    @SerialName("place_name") val placeName: String? = null,
    @SerialName("place_id") val placeId: String? = null,
    @SerialName("activity_type") val activityType: String? = "place",
    @SerialName("start_time") val startTime: String = "09:00 AM",
    @SerialName("end_time") val endTime: String = "11:30 AM",
    @SerialName("estimated_cost") val estimatedCost: Double = 0.0,
    @SerialName("travel_time_minutes") val travelTimeMinutes: Int = 15,
    @SerialName("is_outdoor") val isOutdoor: Boolean = true,
    @SerialName("transport_mode") val transportMode: String = "Cab",
    @SerialName("dietary_tags") val dietaryTags: List<String> = emptyList()
)

@Serializable
data class GeneratedDayDto(
    @SerialName("day_number") val dayNumber: Int = 1,
    val date: String? = null,
    val title: String? = null,
    val theme: String? = null,
    @SerialName("weather_summary") val weatherSummary: String? = null,
    val notes: String? = null,
    @SerialName("estimated_day_cost") val estimatedDayCost: Double = 0.0,
    val activities: List<GeneratedActivityDto> = emptyList()
)

@Serializable
data class GeneratedItineraryPlanDto(
    @SerialName("id") val id: String? = null,
    @SerialName("trip_id") val tripId: String? = null,
    val destination: String = "",
    @SerialName("destination_id") val destinationId: String? = null,
    @SerialName("destination_name") val destinationName: String? = null,
    val title: String = "",
    @SerialName("trip_title") val tripTitle: String? = null,
    @SerialName("duration_days") val durationDays: Int = 1,
    val travelers: Int = 2,
    @SerialName("total_estimated_cost") val totalEstimatedCost: Double = 0.0,
    val currency: String = "INR",
    val days: List<GeneratedDayDto> = emptyList(),
    @SerialName("budget_breakdown") val budgetBreakdown: BudgetBreakdownDto? = null,
    val summary: String? = null,
    val highlights: List<String> = emptyList(),
    @SerialName("safety_notes") val safetyNotes: List<String> = emptyList(),
    @SerialName("packing_tips") val packingTips: List<String> = emptyList(),
    @SerialName("local_tips") val localTips: List<String> = emptyList(),
    @SerialName("model_used") val modelUsed: String? = null,
    @SerialName("is_fallback") val isFallback: Boolean = false
) {
    val effectiveTripId: String? get() = tripId ?: id
    val totalEstimatedBudget: Double get() = totalEstimatedCost
}

// --- Community DTOs ---

@Serializable
data class CommunityForumDto(
    val id: String,
    val title: String,
    val slug: String? = null,
    val description: String? = null,
    @SerialName("cover_image") val coverImage: String? = null,
    val category: String? = null,
    @SerialName("member_count") val memberCount: Int = 0
)

@Serializable
data class CommunityPostDto(
    val id: String,
    @SerialName("forum_id") val forumId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String = "Traveler",
    @SerialName("user_avatar") val userAvatar: String? = null,
    val title: String,
    val content: String,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CreatorItineraryDto(
    val id: String,
    @SerialName("creator_name") val creatorName: String,
    @SerialName("creator_avatar") val creatorAvatar: String? = null,
    val title: String,
    @SerialName("destination_name") val destinationName: String,
    @SerialName("duration_days") val durationDays: Int,
    @SerialName("total_estimated_cost") val totalEstimatedCost: Double = 0.0,
    val price: Double = 0.0,
    val rating: Double = 4.8,
    @SerialName("copy_count") val copyCount: Int = 0
)

// --- Bookings & Payments DTOs ---

@Serializable
data class RequestToBookDto(
    @SerialName("item_type") val itemType: String, // hotel, flight, activity
    @SerialName("item_id") val itemId: String,
    @SerialName("item_title") val itemTitle: String,
    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("check_in_date") val checkInDate: String,
    @SerialName("check_out_date") val checkOutDate: String,
    @SerialName("guest_count") val guestCount: Int = 1,
    @SerialName("total_amount") val totalAmount: Double,
    val currency: String = "INR",
    @SerialName("special_requests") val specialRequests: String? = null
)

@Serializable
data class BookingOutDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("item_type") val itemType: String,
    @SerialName("item_id") val itemId: String,
    @SerialName("item_title") val itemTitle: String,
    @SerialName("booking_reference") val bookingReference: String,
    @SerialName("check_in_date") val checkInDate: String,
    @SerialName("check_out_date") val checkOutDate: String? = null,
    @SerialName("guest_count") val guestCount: Int = 1,
    @SerialName("total_amount") val totalAmount: Double,
    val status: String,
    @SerialName("payment_status") val paymentStatus: String = "pending",
    @SerialName("voucher_qr_data") val voucherQrData: String? = null,
    @SerialName("qr_code_base64") val qrCodeBase64: String? = null,
    val currency: String = "INR"
) {
    val qrCode: String?
        get() = voucherQrData ?: qrCodeBase64
}

@Serializable
data class RazorpayOrderCreateDto(
    @SerialName("booking_id") val bookingId: String,
    @SerialName("idempotency_key") val idempotencyKey: String? = null
)

@Serializable
data class RazorpayOrderOutDto(
    @SerialName("order_id") val orderId: String,
    val amount: Double,
    val currency: String = "INR",
    @SerialName("key_id") val keyId: String,
    val status: String
)

@Serializable
data class RazorpayVerifyDto(
    @SerialName("booking_id") val bookingId: String,
    @SerialName("razorpay_order_id") val razorpayOrderId: String,
    @SerialName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerialName("razorpay_signature") val razorpaySignature: String
)

@Serializable
data class PaymentProcessRequestDto(
    @SerialName("booking_id") val bookingId: String,
    @SerialName("payment_method") val paymentMethod: String = "UPI",
    val currency: String = "INR"
)

@Serializable
data class PaymentReceiptOutDto(
    @SerialName("transaction_ref") val transactionRef: String = "",
    @SerialName("booking_id") val bookingId: String = "",
    @SerialName("booking_reference") val bookingReference: String = "",
    @SerialName("item_title") val itemTitle: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    @SerialName("payment_method") val paymentMethod: String = "UPI",
    val status: String = "Succeeded",
    @SerialName("invoice_number") val invoiceNumber: String = "",
    val timestamp: String = "",
    @SerialName("receipt_url") val receiptUrl: String? = null
)

@Serializable
data class DigitalInvoiceDto(
    @SerialName("invoice_number") val invoiceNumber: String = "INV-2026-0001",
    @SerialName("booking_reference") val bookingReference: String = "BKG-REF-001",
    @SerialName("booking_id") val bookingId: String? = null,
    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("item_title") val itemTitle: String = "Complete Tourist Package",
    @SerialName("guest_name") val guestName: String = "Tourist Traveler",
    @SerialName("guest_contact") val guestContact: String = "N/A",
    val dates: String = "",
    val destination: String = "",
    val subtotal: Double = 0.0,
    @SerialName("gst_tax_18_pct") val gstTax18Pct: Double = 0.0,
    @SerialName("platform_fee") val platformFee: Double = 99.0,
    val discount: Double = 0.0,
    @SerialName("total_paid") val totalPaid: Double = 0.0,
    val currency: String = "INR",
    @SerialName("payment_method") val paymentMethod: String = "UPI",
    @SerialName("payment_id") val paymentId: String? = null,
    val status: String = "Paid & Verified",
    @SerialName("issued_at") val issuedAt: String = "",
    @SerialName("receipt_url") val receiptUrl: String? = null
)

// --- Cancellation & Refund DTOs ---

@Serializable
data class CancellationRequestDto(
    @SerialName("booking_id") val bookingId: String,
    @SerialName("cancellation_reason") val cancellationReason: String = "User requested cancellation"
)

@Serializable
data class RefundMilestoneDto(
    val milestone: String,
    val timestamp: String = "",
    val status: String = "Pending"
)

@Serializable
data class RefundStatusDto(
    @SerialName("refund_ref") val refundRef: String = "",
    @SerialName("booking_id") val bookingId: String = "",
    val amount: Double = 0.0,
    val currency: String = "INR",
    val status: String = "Processing",
    val milestones: List<RefundMilestoneDto> = emptyList()
)

// --- Safety & SOS DTOs ---

@Serializable
data class SOSAlertRequestDto(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("emergency_type") val emergencyType: String = "General Emergency"
)

@Serializable
data class SOSAlertResponseDto(
    @SerialName("alert_id") val alertId: String,
    val status: String,
    val message: String,
    @SerialName("dispatched_at") val dispatchedAt: String? = null,
    @SerialName("contacts_notified") val contactsNotified: Int = 0
)

@Serializable
data class DeviceLocationDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val address: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    @SerialName("is_live") val isLive: Boolean = true
)

@Serializable
data class DeviceHeartbeatRequestDto(
    @SerialName("device_id") val deviceId: String = "",
    @SerialName("battery_level") val batteryLevel: Int = 100,
    @SerialName("is_charging") val isCharging: Boolean = false,
    @SerialName("network_status") val networkStatus: String = "ONLINE",
    @SerialName("location") val location: DeviceLocationDto? = null,
    @SerialName("app_activity") val appActivity: String = "FOREGROUND",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class HeartbeatResponseDto(
    val success: Boolean = true,
    val status: String = "ACTIVE",
    @SerialName("device_state") val deviceState: String = "ACTIVE",
    @SerialName("pending_escalations") val pendingEscalations: Int = 0,
    @SerialName("server_timestamp") val serverTimestamp: Long = System.currentTimeMillis(),
    val message: String = "Heartbeat recorded successfully"
)

@Serializable
data class ImSafeRequestDto(
    @SerialName("alert_id") val alertId: String? = null,
    @SerialName("notes") val notes: String? = "Traveler confirmed safe",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class NeedHelpRequestDto(
    @SerialName("trip_id") val tripId: String? = null,
    @SerialName("urgency") val urgency: String = "HIGH",
    val details: String = "Traveler requested immediate safety check",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SafetyActionResponseDto(
    val success: Boolean = true,
    @SerialName("event_id") val eventId: String? = null,
    val status: String = "SUCCESS",
    val message: String = "Operation completed",
    @SerialName("server_timestamp") val serverTimestamp: Long = System.currentTimeMillis()
)

@Serializable
data class LostPhoneRecoverDto(
    val email: String,
    @SerialName("emergency_recovery_pin") val emergencyRecoveryPin: String
)

@Serializable
data class LostPhoneRecoverResponseDto(
    @SerialName("recovery_token") val recoveryToken: String,
    @SerialName("user_name") val userName: String,
    @SerialName("active_trip_name") val activeTripName: String? = null,
    @SerialName("hotel_name") val hotelName: String? = null,
    @SerialName("hotel_address") val hotelAddress: String? = null,
    @SerialName("hotel_phone") val hotelPhone: String? = null,
    @SerialName("support_helpline") val supportHelpline: String = "+91 1800-TOURIST",
    @SerialName("is_card_frozen") val isCardFrozen: Boolean = false
)

// --- Support DTOs ---

@Serializable
data class SupportTicketCreateDto(
    val subject: String,
    val category: String = "General",
    val message: String,
    val priority: String = "Medium",
    @SerialName("booking_id") val bookingId: String? = null
)

@Serializable
data class TicketMessageDto(
    val id: String,
    @SerialName("sender_type") val senderType: String,
    @SerialName("sender_name") val senderName: String,
    val message: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupportTicketOutDto(
    val id: String,
    val category: String,
    val subject: String,
    val status: String,
    val priority: String,
    @SerialName("created_at") val createdAt: String? = null,
    val messages: List<TicketMessageDto> = emptyList()
)

// --- SIM & Tools DTOs ---

@Serializable
data class SimPlanDto(
    val id: String,
    val provider: String,
    @SerialName("data_allowance") val dataAllowance: String,
    @SerialName("validity_days") val validityDays: Int,
    @SerialName("price_inr") val priceInr: Double,
    @SerialName("is_esim") val isEsim: Boolean
)

@Serializable
data class VisaDocumentDto(
    val id: String,
    @SerialName("document_type") val documentType: String,
    val country: String,
    val status: String,
    @SerialName("document_url") val documentUrl: String
)

@Serializable
data class TranslationRequestDto(
    val text: String,
    @SerialName("target_language") val targetLanguage: String,
    @SerialName("source_language") val sourceLanguage: String = "en"
)

@Serializable
data class TranslationResponseDto(
    @SerialName("original_text") val originalText: String,
    @SerialName("translated_text") val translatedText: String,
    @SerialName("target_language") val targetLanguage: String
)

@Serializable
data class MenuScanRequestDto(
    @SerialName("menu_text") val menuText: String,
    @SerialName("user_diet") val userDiet: String = "Vegetarian"
)

@Serializable
data class MenuItemAnalysisDto(
    @SerialName("item_name") val itemName: String = "",
    @SerialName("is_suitable") val isSuitable: Boolean = true,
    val reason: String = "",
    val ingredients: List<String> = emptyList()
)

@Serializable
data class MenuScanResponseDto(
    @SerialName("total_items_analyzed") val totalItemsAnalyzed: Int = 0,
    @SerialName("suitable_items_count") val suitableItemsCount: Int = 0,
    val items: List<MenuItemAnalysisDto> = emptyList(),
    val warnings: List<String> = emptyList(),
    @SerialName("overall_suitability") val overallSuitability: String? = null,
    @SerialName("diet_confidence") val dietConfidence: Int? = null,
    @SerialName("detected_risks") val detectedRisks: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

// --- Insurance DTOs ---

@Serializable
data class InsurancePolicyDto(
    val id: String = "",
    val provider: String = "",
    @SerialName("coverage_details") val coverageDetails: String = "",
    @SerialName("premium_inr") val premiumInr: Double = 0.0,
    @SerialName("coverage_amount_inr") val coverageAmountInr: Double = 0.0
)

// --- Preparation Checklist DTOs ---

@Serializable
data class WeatherPackingItemDto(
    val item: String = "",
    val packed: Boolean = false,
    val reason: String = ""
)

@Serializable
data class CulturalEtiquetteDto(
    val requirement: String = "",
    val guideline: String = ""
)

@Serializable
data class MedicalKitItemDto(
    val item: String = "",
    val purpose: String = ""
)

@Serializable
data class InternationalChecklistItemDto(
    val item: String = "",
    val verified: Boolean = true
)

@Serializable
data class PreparationChecklistDto(
    val destination: String = "",
    @SerialName("weather_packing_list") val weatherPackingList: List<WeatherPackingItemDto> = emptyList(),
    @SerialName("cultural_preparation") val culturalPreparation: List<CulturalEtiquetteDto> = emptyList(),
    @SerialName("travel_medical_kit") val travelMedicalKit: List<MedicalKitItemDto> = emptyList(),
    @SerialName("international_checklist") val internationalChecklist: List<InternationalChecklistItemDto> = emptyList()
)

// --- Transport Brain DTOs ---

@Serializable
data class TransportOptionDto(
    val mode: String = "",
    val badge: String = "",
    @SerialName("estimated_time_mins") val estimatedTimeMins: Int = 0,
    @SerialName("estimated_cost_inr") val estimatedCostInr: Double = 0.0,
    @SerialName("safety_rating") val safetyRating: Double = 5.0,
    @SerialName("crowd_level") val crowdLevel: String = "",
    @SerialName("operating_hours") val operatingHours: String = "",
    val tip: String = ""
)

@Serializable
data class CityPassDto(
    val name: String = "",
    @SerialName("price_1_day") val price1Day: Double = 0.0,
    @SerialName("price_3_day") val price3Day: Double = 0.0,
    val benefits: String = ""
)

@Serializable
data class TransportComparisonResponseDto(
    val city: String = "",
    val route: String = "",
    val options: List<TransportOptionDto> = emptyList(),
    @SerialName("city_pass_recommendation") val cityPassRecommendation: CityPassDto? = null
)


