package com.travellikepro.opsleader.data.api

import retrofit2.Response
import retrofit2.http.*

// --------------------------------------------------
// Base Wrapper & Auth DTOs
// --------------------------------------------------
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val errorCode: String? = null,
    val token: String? = null,
    val user: UserResponse? = null,
    val data: T? = null
)

data class LoginRequest(
    val email: String? = null,
    val phone: String? = null,
    val password: String
)

data class GoogleAuthRequest(
    val idToken: String,
    val referenceId: String? = null
)

data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val errorCode: String? = null,
    val token: String? = null,
    val user: UserResponse? = null,
    val data: Any? = null
)

data class UserResponse(
    val id: String? = null,
    val name: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val role: String? = null,
    val phone: String? = null,
    val region: String? = null,
    val photoUrl: String? = null
) {
    val displayName: String
        get() = fullName?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() } ?: email?.substringBefore("@") ?: "Ops Leader"
}

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String = "Password@123",
    val referenceId: String,
    val googleIdToken: String? = null,
    val role: String? = "OPS_LEADER"
)

data class SignupResponse(
    val success: Boolean,
    val message: String?,
    val errorCode: String?,
    val user: UserResponse?
)

// --------------------------------------------------
// V1: Requests + Vendors + Assignment DTOs
// --------------------------------------------------
data class TripDto(
    val id: String,
    val tourist_id: String? = null,
    val tourist_name: String? = null,
    val requester_name: String? = null,
    val requester_contact: String? = null,
    val tourist_phone: String? = null,
    val destination: String? = null,
    val destination_name: String? = null,
    val title: String? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val preferred_dates: String? = null,
    val group_size: Int = 1,
    val number_of_guests: Int = 1,
    val status: String? = "pending", // pending, accepted, in_progress, completed, cancelled, rejected
    val budget: Double = 0.0,
    val estimated_budget: Double = 0.0,
    val assigned_ops_leader_id: String? = null,
    val assigned_vendor_id: String? = null,
    val assigned_vendor_name: String? = null,
    val special_requirements: String? = null,
    val special_notes: String? = null,
    val created_at: String? = null
)

data class AssignTripRequest(
    val ops_leader_id: String? = null,
    val vendor_id: String? = null,
    val vendor_name: String? = null,
    val notes: String? = null
)

data class AddNoteRequest(
    val content: String
)

data class VendorDto(
    val id: String,
    val name: String,
    val service_type: String? = null, // guide, hotel, transport, activity, restaurant
    val serviceType: String? = null,
    val contact_phone: String? = null,
    val contactPhone: String? = null,
    val email: String? = null,
    val region_coverage: List<String> = emptyList(),
    val regionCoverage: List<String> = emptyList(),
    val availability_status: String = "available", // available, at_capacity, unavailable
    val availabilityStatus: String = "AVAILABLE",
    val quality_score: Double = 5.0,
    val qualityScore: Double = 5.0,
    val active_trips_count: Int = 0
)

data class TeamMemberDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val status: String = "active",
    val assigned_trips_count: Int = 0
)

// --------------------------------------------------
// V2: Bookings + Notifications + Support DTOs
// --------------------------------------------------
data class BookingDto(
    val id: String,
    val trip_id: String? = null,
    val tourist_name: String? = null,
    val tourist_contact: String? = null,
    val destination: String? = null,
    val dates: String? = null,
    val guests_count: Int = 1,
    val total_amount: Double = 0.0,
    val payment_status: String = "paid", // paid, partial, pending
    val booking_status: String = "confirmed", // confirmed, completed, cancelled
    val hotel_partner: String? = null,
    val transport_partner: String? = null,
    val guide_partner: String? = null
)

data class NotificationDto(
    val id: String,
    val title: String,
    val message: String,
    val type: String = "SYSTEM", // SOS, ASSIGNMENT, BOOKING, SYSTEM, WEATHER
    val is_read: Boolean = false,
    val timestamp: String? = null,
    val created_at: String? = null
)

data class BroadcastNotificationRequest(
    val topic: String = "ops_broadcasts",
    val title: String,
    val body: String,
    val severity: String = "NORMAL"
)

data class SupportTicketDto(
    val id: String,
    val tourist_id: String? = null,
    val tourist_name: String? = null,
    val subject: String,
    val category: String = "GENERAL", // BOOKING, TRANSPORT, EMERGENCY, GENERAL
    val priority: String = "MEDIUM", // CRITICAL, URGENT, HIGH, MEDIUM, LOW
    val status: String = "open", // open, in_progress, resolved
    val messages: List<TicketMessageDto> = emptyList(),
    val created_at: String? = null,
    val updated_at: String? = null
)

data class TicketMessageDto(
    val id: String,
    val sender_id: String? = null,
    val sender_name: String? = null,
    val sender_role: String? = "OPS_LEADER",
    val message: String,
    val timestamp: String? = null,
    val created_at: String? = null
)

data class SendTicketMessageRequest(
    val message: String
)

// --------------------------------------------------
// V3: SOS + Incidents + Safety + Weather DTOs
// --------------------------------------------------
data class SosAlertDto(
    val id: String = "",
    val tourist_id: String? = null,
    val tourist_name: String? = null,
    val tourist_phone: String? = null,
    val location_name: String? = null,
    val location: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val emergency_type: String? = "MEDICAL", // MEDICAL, ACCIDENT, THEFT, LOST, HARASSMENT
    val severity: String? = "CRITICAL", // CRITICAL, HIGH, MEDIUM
    val status: String? = "active", // active, responding, acknowledged, resolved
    val responder: String? = null,
    val dispatched_responder: String? = null,
    val reported_at: String? = null,
    val created_at: String? = null,
    val notes: String? = null
)

data class DispatchResponderRequest(
    val responder_name: String? = null,
    val responder_phone: String? = null,
    val responder: String? = null,
    val eta_minutes: Int? = 15,
    val notes: String? = null
)

data class IncidentDto(
    val id: String = "",
    val trip_id: String? = null,
    val tourist_name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
    val category: String? = "SAFETY",
    val severity: String? = "MAJOR", // CRITICAL, MAJOR, MINOR
    val status: String? = "open", // open, investigating, resolved
    val destination: String? = null,
    val location: String? = null,
    val reported_at: String? = null,
    val created_at: String? = null,
    val resolved_at: String? = null,
    val resolution_summary: String? = null
)

data class CreateIncidentRequest(
    val trip_id: String? = null,
    val tourist_name: String,
    val title: String,
    val description: String,
    val category: String = "SAFETY",
    val severity: String = "MAJOR",
    val location: String
)

data class WeatherForecastDto(
    val region: String? = "Default Region",
    val current_temp_celsius: Int = 30,
    val condition: String? = "SUNNY",
    val humidity_percent: Int = 40,
    val advisory_notice: String? = null,
    val is_alert_active: Boolean = false
)

// --------------------------------------------------
// V4: Analytics + Reports + Audit + Tourists DTOs
// --------------------------------------------------
data class DashboardKpiDto(
    val activeTourists: Int = 0,
    val activeTrips: Int = 0,
    val activeSos: Int = 0,
    val openIncidents: Int = 0,
    val criticalAlerts: Int = 0,
    val openSupportTickets: Int = 0,
    val total_active_trips: Int = 0,
    val total_active_tourists: Int = 0,
    val active_trips: Int = 0,
    val active_sos: Int = 0,
    val open_incidents: Int = 0,
    val critical_alerts: Int = 0,
    val open_support_tickets: Int = 0
)

data class DashboardStatsDto(
    val tourists: Map<String, Int>? = null,
    val trips: Map<String, Int>? = null,
    val bookings: Map<String, Int>? = null,
    val support: Map<String, Int>? = null,
    val safety: Map<String, Int>? = null,
    val weather: Map<String, Int>? = null,
    val partners: Map<String, Int>? = null,
    val alerts: Map<String, Int>? = null
)

data class OperationalReportDto(
    val id: String,
    val title: String,
    val period: String,
    val total_trips: Int = 0,
    val total_revenue: Double = 0.0,
    val incident_count: Int = 0,
    val average_resolution_time_minutes: Int = 0,
    val generated_at: String? = null
)

data class TouristDto(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val nationality: String? = "International",
    val status: String? = "active",
    val active_booking_id: String? = null,
    val current_destination: String? = null,
    val destination: String? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_phone: String? = null,
    val medical_notes: String? = null
)

// --------------------------------------------------
// API Service Interface
// --------------------------------------------------
interface ApiService {

    // Auth
    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponse>>

    @POST("auth/google")
    suspend fun googleAuth(
        @Body request: GoogleAuthRequest
    ): Response<AuthResponse>

    @POST("auth/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<SignupResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<UserResponse>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    // Dashboard
    @GET("dashboard/stats")
    suspend fun getDashboardStats(): Response<ApiResponse<DashboardStatsDto>>

    @GET("dashboard/kpis")
    suspend fun getDashboardKpis(): Response<ApiResponse<DashboardKpiDto>>

    @GET("dashboard/charts/{type}")
    suspend fun getChartData(
        @Path("type") type: String,
        @Query("days") days: Int = 7
    ): Response<ApiResponse<List<Map<String, Any>>>>

    // V1: Trips & Requests
    @GET("trips")
    suspend fun getTrips(
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<TripDto>>>

    @GET("trips/{id}")
    suspend fun getTripById(
        @Path("id") tripId: String
    ): Response<ApiResponse<TripDto>>

    @POST("trips/{id}/assign")
    suspend fun assignTrip(
        @Path("id") tripId: String,
        @Body request: AssignTripRequest
    ): Response<ApiResponse<TripDto>>

    @POST("trips/{id}/notes")
    suspend fun addTripNote(
        @Path("id") tripId: String,
        @Body request: AddNoteRequest
    ): Response<ApiResponse<Any>>

    // V1: Partners / Vendors
    @GET("partners")
    suspend fun getPartners(
        @Query("serviceType") serviceType: String? = null
    ): Response<ApiResponse<List<VendorDto>>>

    @GET("partners/{id}")
    suspend fun getPartnerById(
        @Path("id") partnerId: String
    ): Response<ApiResponse<VendorDto>>

    // V1: Team Members
    @GET("team")
    suspend fun getTeam(): Response<ApiResponse<List<TeamMemberDto>>>

    // V2: Bookings
    @GET("bookings")
    suspend fun getBookings(
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<BookingDto>>>

    @GET("bookings/{id}")
    suspend fun getBookingById(
        @Path("id") bookingId: String
    ): Response<ApiResponse<BookingDto>>

    // V2: Notifications
    @GET("notifications")
    suspend fun getNotifications(): Response<ApiResponse<List<NotificationDto>>>

    @POST("notifications/broadcast")
    suspend fun broadcastNotification(
        @Body request: BroadcastNotificationRequest
    ): Response<ApiResponse<Unit>>

    // V2: Support Tickets
    @GET("support/tickets")
    suspend fun getSupportTickets(
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<SupportTicketDto>>>

    @GET("support/tickets/{id}")
    suspend fun getSupportTicketById(
        @Path("id") ticketId: String
    ): Response<ApiResponse<SupportTicketDto>>

    @POST("support/tickets/{id}/messages")
    suspend fun sendTicketMessage(
        @Path("id") ticketId: String,
        @Body request: SendTicketMessageRequest
    ): Response<ApiResponse<SupportTicketDto>>

    // V3: SOS Emergencies
    @GET("sos")
    suspend fun getSosAlerts(
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<SosAlertDto>>>

    @POST("sos/{id}/dispatch")
    suspend fun dispatchSosResponder(
        @Path("id") alertId: String,
        @Body request: DispatchResponderRequest
    ): Response<ApiResponse<SosAlertDto>>

    @POST("sos/{id}/resolve")
    suspend fun resolveSosAlert(
        @Path("id") alertId: String,
        @Body notes: Map<String, String>
    ): Response<ApiResponse<SosAlertDto>>

    // V3: Incidents
    @GET("incidents")
    suspend fun getIncidents(
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<IncidentDto>>>

    @POST("incidents")
    suspend fun createIncident(
        @Body request: CreateIncidentRequest
    ): Response<ApiResponse<IncidentDto>>

    @GET("incidents/{id}")
    suspend fun getIncidentById(
        @Path("id") incidentId: String
    ): Response<ApiResponse<IncidentDto>>

    @PUT("incidents/{id}/status")
    suspend fun updateIncidentStatus(
        @Path("id") incidentId: String,
        @Body body: Map<String, String>
    ): Response<ApiResponse<IncidentDto>>

    @POST("incidents/{id}/resolve")
    suspend fun resolveIncident(
        @Path("id") incidentId: String,
        @Body body: Map<String, String>
    ): Response<ApiResponse<IncidentDto>>

    // V3: Safety & Weather
    @GET("safety/checklists")
    suspend fun getSafetyChecklists(): Response<ApiResponse<List<Map<String, Any>>>>

    @GET("weather/alerts")
    suspend fun getWeatherAlerts(): Response<ApiResponse<List<Map<String, Any>>>>

    // V4: Tourists
    @GET("tourists")
    suspend fun getTourists(): Response<ApiResponse<List<TouristDto>>>

    @GET("tourists/{id}")
    suspend fun getTouristById(
        @Path("id") touristId: String
    ): Response<ApiResponse<TouristDto>>
}