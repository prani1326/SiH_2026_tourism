package com.touristapp.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Walkthrough : Screen("walkthrough")
    data object Login : Screen("login")
    data object Signup : Screen("signup")
    data object ForgotPassword : Screen("forgot_password")
    data object Otp : Screen("otp/{emailOrPhone}") {
        fun createRoute(emailOrPhone: String) = "otp/$emailOrPhone"
    }
    data object Onboarding : Screen("onboarding")
    data object ProfileOnboarding : Screen("profile_onboarding?step={step}") {
        fun createRoute(step: Int = 1) = "profile_onboarding?step=$step"
    }
    
    // Main App
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Explore : Screen("explore")
    data object MapScreen : Screen("map?destinationId={destinationId}") {
        fun createRoute(destinationId: String? = null): String {
            return if (!destinationId.isNullOrBlank() && destinationId != "{destinationId}") {
                "map?destinationId=$destinationId"
            } else {
                "map"
            }
        }
    }
    data object TrackTrip : Screen("track_trip?tripId={tripId}") {
        fun createRoute(tripId: String? = null): String {
            return if (!tripId.isNullOrBlank() && tripId != "{tripId}") {
                "track_trip?tripId=$tripId"
            } else {
                "track_trip"
            }
        }
    }
    
    // Details
    data object DestinationDetail : Screen("destination/{id}") {
        fun createRoute(id: String) = "destination/$id"
    }
    
    // Trips & Bookings
    data object AiPlanner : Screen("ai_planner?destination={destination}") {
        fun createRoute(destination: String? = null): String {
            return if (!destination.isNullOrBlank() && destination != "{destination}") {
                val encoded = java.net.URLEncoder.encode(destination, "UTF-8")
                "ai_planner?destination=$encoded"
            } else {
                "ai_planner"
            }
        }
    }
    data object Itinerary : Screen("itinerary/{id}") {
        fun createRoute(id: String) = "itinerary/$id"
    }
    data object TrueCost : Screen("true_cost")
    data object TripCard : Screen("trip_card/{id}") {
        fun createRoute(id: String) = "trip_card/$id"
    }
    
    // Safety & Security System
    data object Safety : Screen("safety")
    data object EmergencySos : Screen("emergency_sos?tripId={tripId}") {
        fun createRoute(tripId: String? = null): String {
            return if (!tripId.isNullOrBlank() && tripId != "{tripId}") {
                "emergency_sos?tripId=$tripId"
            } else {
                "emergency_sos"
            }
        }
    }
    data object ActiveSos : Screen("active_sos/{alertId}") {
        fun createRoute(alertId: String) = "active_sos/$alertId"
    }
    data object EmergencyContacts : Screen("emergency_contacts")
    data object NearbyHelp : Screen("nearby_help")
    data object LostPhoneSetup : Screen("lost_phone_setup")
    data object SafetyGroupConfig : Screen("safety_group_config")
    data object IncidentReport : Screen("incident_report?tripId={tripId}") {
        fun createRoute(tripId: String? = null): String {
            return if (!tripId.isNullOrBlank() && tripId != "{tripId}") {
                "incident_report?tripId=$tripId"
            } else {
                "incident_report"
            }
        }
    }

    data object Profile : Screen("profile")
    data object Bookings : Screen("bookings")
    data object BookingReview : Screen("booking_review/{tripId}") {
        fun createRoute(tripId: String) = "booking_review/$tripId"
    }
    data object Payment : Screen("payment/{tripId}?amount={amount}") {
        fun createRoute(tripId: String, amount: Double) = "payment/$tripId?amount=$amount"
    }
    data object BookingConfirmation : Screen("booking_confirmation/{bookingId}") {
        fun createRoute(bookingId: String) = "booking_confirmation/$bookingId"
    }
    data object Invoice : Screen("invoice/{bookingId}") {
        fun createRoute(bookingId: String) = "invoice/$bookingId"
    }
    data object Support : Screen("support")
    data object TravelTools : Screen("travel_tools")
    data object Guide : Screen("guide")
    data object GuideCameraTranslate : Screen("guide_camera_translate")
    data object GuidePlaceDetection : Screen("guide_place_detection")
    data object GuideVoiceTranslate : Screen("guide_voice_translate")
    data object GuideTravelInfo : Screen("guide_travel_info")
    data object GuideSaved : Screen("guide_saved")
    data object AiAssistant : Screen("ai_assistant?initialPrompt={initialPrompt}") {
        fun createRoute(initialPrompt: String? = null): String {
            return if (!initialPrompt.isNullOrBlank() && initialPrompt != "{initialPrompt}") {
                val encoded = java.net.URLEncoder.encode(initialPrompt, "UTF-8")
                "ai_assistant?initialPrompt=$encoded"
            } else {
                "ai_assistant"
            }
        }
    }

    // SIH Smart Tourist OS Routes
    data object SafetyIntelligence : Screen("safety_intelligence?destinationId={destinationId}") {
        fun createRoute(destinationId: String? = null): String {
            return if (!destinationId.isNullOrBlank() && destinationId != "{destinationId}") {
                "safety_intelligence?destinationId=$destinationId"
            } else {
                "safety_intelligence"
            }
        }
    }
    data object Guardian : Screen("guardian?tripId={tripId}") {
        fun createRoute(tripId: String? = null): String {
            return if (!tripId.isNullOrBlank() && tripId != "{tripId}") {
                "guardian?tripId=$tripId"
            } else {
                "guardian"
            }
        }
    }
    data object ScamShield : Screen("scam_shield?destinationId={destinationId}") {
        fun createRoute(destinationId: String? = null): String {
            return if (!destinationId.isNullOrBlank() && destinationId != "{destinationId}") {
                "scam_shield?destinationId=$destinationId"
            } else {
                "scam_shield"
            }
        }
    }
    data object HeritageLens : Screen("heritage_lens")
}