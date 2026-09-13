package com.touristapp.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class MessageSender {
    USER,
    AI,
    SYSTEM
}

@Serializable
enum class ChatCardType {
    NONE,
    PLACE,
    BOOKING,
    ITINERARY,
    BUDGET,
    SAFETY,
    CONFIRMATION,
    NAV_ACTION,
    VENDOR_MESSAGE
}

@Serializable
enum class ChatIntent {
    TRAVEL_GENERAL,
    DESTINATION_DISCOVERY,
    TRIP_PLANNING,
    ITINERARY_QUERY,
    ITINERARY_UPDATE,
    BOOKING_QUERY,
    BOOKING_ACTION,
    BUDGET_QUERY,
    BUDGET_CALCULATION,
    NEARBY_SEARCH,
    FOOD_SEARCH,
    HOTEL_SEARCH,
    TRANSPORT,
    GUIDE_QUERY,
    VENDOR_QUERY,
    MESSAGE_GUIDE,
    MESSAGE_VENDOR,
    TRANSLATION,
    WEATHER,
    SAFETY,
    SOS,
    LOST_PHONE,
    SAFETY_GROUP,
    APP_NAVIGATION,
    PROFILE,
    GENERAL_CHAT
}

@Serializable
enum class FeedbackStatus {
    NONE,
    HELPFUL,
    NOT_HELPFUL
}

@Serializable
data class PlaceCardPayload(
    val id: String = "",
    val name: String = "",
    val category: String = "Attraction",
    val distance: String = "",
    val address: String = "",
    val rating: Double = 4.5,
    val openStatus: String = "Open Now",
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class BookingCardPayload(
    val id: String = "",
    val title: String = "",
    val reference: String = "",
    val itemType: String = "Hotel",
    val dateOrTime: String = "",
    val status: String = "Confirmed",
    val guests: Int = 1,
    val totalAmount: Double = 0.0,
    val contactPhone: String? = null
)

@Serializable
data class ItineraryActivityPayload(
    val time: String = "",
    val title: String = "",
    val location: String = "",
    val estimatedCost: String = "",
    val duration: String = ""
)

@Serializable
data class ItineraryCardPayload(
    val tripTitle: String = "",
    val destination: String = "",
    val dayNumber: Int = 1,
    val date: String = "",
    val activities: List<ItineraryActivityPayload> = emptyList()
)

@Serializable
data class BudgetCardPayload(
    val totalBudget: Double = 0.0,
    val spentAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val proposedItem: String? = null,
    val proposedCost: Double? = null,
    val remainingAfter: Double? = null,
    val isAffordable: Boolean = true
)

@Serializable
data class SafetyCardPayload(
    val alertType: String = "Safety Guidance",
    val description: String = "",
    val emergencyNumber: String = "112",
    val canTriggerSos: Boolean = true,
    val showLostPhoneGuide: Boolean = false,
    val showSafetyGroupGuide: Boolean = false
)

@Serializable
data class VendorMessagePayload(
    val recipientType: String = "Guide", // "Hotel", "Guide", "Driver", "Vendor"
    val recipientName: String = "",
    val messagePreview: String = "",
    val recipientPhone: String? = null
)

@Serializable
data class ConfirmationPayload(
    val actionType: String = "", // "REMOVE_ACTIVITY", "ADD_ACTIVITY", "MODIFY_TRIP", "SEND_MESSAGE", "CANCEL_BOOKING"
    val title: String = "",
    val description: String = "",
    val targetItemId: String = "",
    val targetDayNumber: Int = 1,
    val extraData: Map<String, String> = emptyMap()
)

@Serializable
data class NavActionPayload(
    val destinationScreen: String = "", // "trips", "bookings", "safety", "map", "profile", "explore", "travel_tools"
    val title: String = "",
    val label: String = ""
)

@Serializable
data class ShortTermContext(
    var destination: String? = null,
    var durationDays: Int? = null,
    var pace: String? = null,
    var budgetTier: String? = null,
    var dietPreference: String? = null,
    var pendingAction: String? = null,
    var lastIntent: ChatIntent = ChatIntent.GENERAL_CHAT
)

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String = "default",
    val sender: MessageSender = MessageSender.AI,
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val cardType: ChatCardType = ChatCardType.NONE,
    val intent: ChatIntent = ChatIntent.GENERAL_CHAT,
    val placePayload: PlaceCardPayload? = null,
    val bookingPayload: BookingCardPayload? = null,
    val itineraryPayload: ItineraryCardPayload? = null,
    val budgetPayload: BudgetCardPayload? = null,
    val safetyPayload: SafetyCardPayload? = null,
    val vendorMessagePayload: VendorMessagePayload? = null,
    val confirmationPayload: ConfirmationPayload? = null,
    val navPayload: NavActionPayload? = null,
    val isConfirmationPending: Boolean = false,
    val toolUsed: String? = null,
    val feedbackStatus: FeedbackStatus = FeedbackStatus.NONE,
    val feedbackReason: String? = null
)

@Serializable
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "New Trip Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastMessage: String = ""
)

data class ChatQuickAction(
    val id: String,
    val iconEmoji: String,
    val title: String,
    val subtitle: String,
    val prompt: String
)
