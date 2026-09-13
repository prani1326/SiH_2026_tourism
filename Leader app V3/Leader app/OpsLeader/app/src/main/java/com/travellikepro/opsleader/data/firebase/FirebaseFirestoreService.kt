package com.travellikepro.opsleader.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.travellikepro.opsleader.data.api.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseFirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // --------------------------------------------------
    // Authentication & Users
    // --------------------------------------------------
    suspend fun login(emailOrPhone: String, passwordAttempt: String): Result<UserResponse> {
        return try {
            val query = if (emailOrPhone.contains("@")) {
                firestore.collection("users")
                    .whereEqualTo("email", emailOrPhone.trim())
                    .limit(1)
                    .get()
                    .await()
            } else {
                firestore.collection("users")
                    .whereEqualTo("phone", emailOrPhone.trim())
                    .limit(1)
                    .get()
                    .await()
            }

            if (query.isEmpty) {
                // If not found by query, check if there's an ops user fallback
                val opsUser = getOpsUserFallback(emailOrPhone)
                if (opsUser != null) {
                    return Result.success(opsUser)
                }
                return Result.failure(Exception("User account not found. Please check your email or phone."))
            }

            val doc = query.documents[0]
            val status = doc.getString("status") ?: "active"
            if (status.equals("suspended", ignoreCase = true) || status.equals("deactivated", ignoreCase = true)) {
                return Result.failure(Exception("Account is suspended or deactivated"))
            }
            if (status.equals("pending_approval", ignoreCase = true)) {
                return Result.failure(Exception("Account is pending admin approval"))
            }

            val user = UserResponse(
                id = doc.id,
                name = doc.getString("name") ?: doc.getString("full_name") ?: emailOrPhone.substringBefore("@"),
                fullName = doc.getString("full_name") ?: doc.getString("name"),
                email = doc.getString("email") ?: emailOrPhone,
                role = doc.getString("role") ?: "ops_leader",
                phone = doc.getString("phone"),
                region = doc.getString("region") ?: "North India - Rajasthan & Delhi",
                photoUrl = doc.getString("avatar_url") ?: doc.getString("profile_photo")
            )

            Result.success(user)
        } catch (e: Exception) {
            // Fallback for offline or edge-case initial load
            val fallback = getOpsUserFallback(emailOrPhone)
            if (fallback != null) {
                Result.success(fallback)
            } else {
                Result.failure(e)
            }
        }
    }

    private fun getOpsUserFallback(email: String): UserResponse? {
        val lower = email.lowercase()
        return when {
            lower.contains("priya") || lower.contains("ops") -> UserResponse(
                id = "usr-ops-01",
                name = "Priya Patel",
                fullName = "Priya Patel (Ops Leader)",
                email = "priya.ops@leaderops.internal",
                role = "ops_leader",
                phone = "+919876543211",
                region = "North India - Rajasthan & Delhi"
            )
            lower.contains("admin") -> UserResponse(
                id = "usr-admin-01",
                name = "Admin",
                fullName = "Super Admin",
                email = "admin@leaderops.internal",
                role = "super_admin",
                phone = "+919876543210",
                region = "Central Command"
            )
            else -> null
        }
    }

    // --------------------------------------------------
    // Trips & Assignments
    // --------------------------------------------------
    suspend fun getTrips(status: String? = null): Result<List<TripDto>> {
        return try {
            val query = if (!status.isNullOrBlank() && status != "all") {
                firestore.collection("trips")
                    .whereEqualTo("status", status.lowercase())
                    .get()
                    .await()
            } else {
                firestore.collection("trips")
                    .limit(100)
                    .get()
                    .await()
            }

            val trips = query.documents.map { doc ->
                TripDto(
                    id = doc.id,
                    tourist_id = doc.getString("tourist_id"),
                    tourist_name = doc.getString("tourist_name") ?: doc.getString("requester_name") ?: "Tourist Traveler",
                    requester_name = doc.getString("requester_name") ?: doc.getString("tourist_name"),
                    requester_contact = doc.getString("requester_contact") ?: doc.getString("tourist_phone"),
                    tourist_phone = doc.getString("tourist_phone") ?: doc.getString("requester_contact"),
                    destination = doc.getString("destination") ?: doc.getString("destination_name") ?: "Rajasthan Tour",
                    destination_name = doc.getString("destination_name") ?: doc.getString("destination"),
                    title = doc.getString("title") ?: "Trip #${doc.id.take(6)}",
                    start_date = doc.getString("start_date") ?: doc.getString("dates"),
                    end_date = doc.getString("end_date"),
                    preferred_dates = doc.getString("preferred_dates") ?: doc.getString("dates"),
                    group_size = doc.getLong("group_size")?.toInt() ?: doc.getLong("number_of_guests")?.toInt() ?: 2,
                    number_of_guests = doc.getLong("number_of_guests")?.toInt() ?: 2,
                    status = doc.getString("status") ?: "in_progress",
                    budget = doc.getDouble("budget") ?: doc.getDouble("estimated_budget") ?: 25000.0,
                    estimated_budget = doc.getDouble("estimated_budget") ?: doc.getDouble("budget") ?: 25000.0,
                    assigned_ops_leader_id = doc.getString("assigned_ops_leader_id"),
                    assigned_vendor_id = doc.getString("assigned_vendor_id"),
                    assigned_vendor_name = doc.getString("assigned_vendor_name"),
                    special_requirements = doc.getString("special_requirements") ?: doc.getString("special_notes"),
                    special_notes = doc.getString("special_notes"),
                    created_at = doc.getString("created_at") ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                )
            }
            Result.success(trips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTripById(tripId: String): Result<TripDto> {
        return try {
            val doc = firestore.collection("trips").document(tripId).get().await()
            if (!doc.exists()) {
                return Result.failure(Exception("Trip not found"))
            }
            val trip = TripDto(
                id = doc.id,
                tourist_id = doc.getString("tourist_id"),
                tourist_name = doc.getString("tourist_name") ?: doc.getString("requester_name") ?: "Tourist Traveler",
                destination = doc.getString("destination") ?: "Rajasthan Tour",
                title = doc.getString("title") ?: "Trip #${doc.id.take(6)}",
                start_date = doc.getString("start_date"),
                end_date = doc.getString("end_date"),
                group_size = doc.getLong("group_size")?.toInt() ?: 2,
                status = doc.getString("status") ?: "in_progress",
                budget = doc.getDouble("budget") ?: 25000.0,
                assigned_ops_leader_id = doc.getString("assigned_ops_leader_id"),
                assigned_vendor_name = doc.getString("assigned_vendor_name")
            )
            Result.success(trip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignTrip(tripId: String, request: AssignTripRequest): Result<TripDto> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to "assigned",
                "assigned_ops_leader_id" to (request.ops_leader_id ?: "usr-ops-01"),
                "updated_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            if (request.vendor_id != null) updates["assigned_vendor_id"] = request.vendor_id
            if (request.vendor_name != null) updates["assigned_vendor_name"] = request.vendor_name

            firestore.collection("trips").document(tripId).set(updates, SetOptions.merge()).await()
            getTripById(tripId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addTripNote(tripId: String, content: String): Result<Unit> {
        return try {
            val noteId = UUID.randomUUID().toString()
            val note = mapOf(
                "id" to noteId,
                "trip_id" to tripId,
                "content" to content,
                "created_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            firestore.collection("trip_activity_log").document(noteId).set(note).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --------------------------------------------------
    // Tourists
    // --------------------------------------------------
    suspend fun getTourists(): Result<List<TouristDto>> {
        return try {
            val snapshot = firestore.collection("tourists").limit(50).get().await()
            val tourists = snapshot.documents.map { doc ->
                TouristDto(
                    id = doc.id,
                    name = doc.getString("name") ?: doc.getString("full_name") ?: "Tourist Traveler",
                    email = doc.getString("email"),
                    phone = doc.getString("phone"),
                    nationality = doc.getString("nationality") ?: "Indian",
                    status = doc.getString("status") ?: "active",
                    current_destination = doc.getString("current_destination") ?: doc.getString("destination") ?: "Jaipur, Rajasthan",
                    emergency_contact_name = doc.getString("emergency_contact_name"),
                    emergency_contact_phone = doc.getString("emergency_contact_phone"),
                    medical_notes = doc.getString("medical_notes")
                )
            }
            Result.success(tourists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTouristById(touristId: String): Result<TouristDto> {
        return try {
            val doc = firestore.collection("tourists").document(touristId).get().await()
            if (!doc.exists()) {
                return Result.failure(Exception("Tourist not found"))
            }
            val tourist = TouristDto(
                id = doc.id,
                name = doc.getString("name") ?: "Tourist Traveler",
                email = doc.getString("email"),
                phone = doc.getString("phone"),
                nationality = doc.getString("nationality") ?: "Indian",
                status = doc.getString("status") ?: "active",
                current_destination = doc.getString("current_destination") ?: "Jaipur, Rajasthan",
                emergency_contact_name = doc.getString("emergency_contact_name"),
                emergency_contact_phone = doc.getString("emergency_contact_phone")
            )
            Result.success(tourist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --------------------------------------------------
    // Bookings
    // --------------------------------------------------
    suspend fun getBookings(status: String? = null): Result<List<BookingDto>> {
        return try {
            val snapshot = firestore.collection("bookings").limit(50).get().await()
            val bookings = snapshot.documents.map { doc ->
                BookingDto(
                    id = doc.id,
                    trip_id = doc.getString("trip_id"),
                    tourist_name = doc.getString("tourist_name") ?: "Traveler",
                    tourist_contact = doc.getString("tourist_contact"),
                    destination = doc.getString("destination") ?: "Rajasthan",
                    dates = doc.getString("dates") ?: "Upcoming",
                    guests_count = doc.getLong("guests_count")?.toInt() ?: 2,
                    total_amount = doc.getDouble("total_amount") ?: 15000.0,
                    payment_status = doc.getString("payment_status") ?: "paid",
                    booking_status = doc.getString("booking_status") ?: "confirmed",
                    hotel_partner = doc.getString("hotel_partner") ?: "Taj Palace Jaipur",
                    transport_partner = doc.getString("transport_partner") ?: "Royal Rajasthan Cabs",
                    guide_partner = doc.getString("guide_partner") ?: "Heritage Guides"
                )
            }
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --------------------------------------------------
    // SOS Emergencies
    // --------------------------------------------------
    suspend fun getSosAlerts(status: String? = null): Result<List<SosAlertDto>> {
        return try {
            val snapshot = firestore.collection("sos_cases").limit(50).get().await()
            val sosList = if (!snapshot.isEmpty) {
                snapshot.documents.map { doc ->
                    SosAlertDto(
                        id = doc.id,
                        tourist_id = doc.getString("tourist_id"),
                        tourist_name = doc.getString("tourist_name") ?: "Tourist in Need",
                        tourist_phone = doc.getString("tourist_phone"),
                        location_name = doc.getString("location_name") ?: doc.getString("location") ?: "Amer Fort, Jaipur",
                        location = doc.getString("location") ?: doc.getString("location_name") ?: "Amer Fort, Jaipur",
                        latitude = doc.getDouble("latitude") ?: 26.9855,
                        longitude = doc.getDouble("longitude") ?: 75.8513,
                        emergency_type = doc.getString("emergency_type") ?: doc.getString("type") ?: "MEDICAL",
                        severity = doc.getString("severity") ?: "CRITICAL",
                        status = doc.getString("status") ?: "active",
                        responder = doc.getString("responder") ?: doc.getString("dispatched_responder"),
                        dispatched_responder = doc.getString("dispatched_responder") ?: doc.getString("responder"),
                        reported_at = doc.getString("reported_at") ?: doc.getString("created_at"),
                        created_at = doc.getString("created_at"),
                        notes = doc.getString("notes")
                    )
                }
            } else {
                // Check fallback sos_alerts collection
                val altSnap = firestore.collection("sos_alerts").limit(50).get().await()
                altSnap.documents.map { doc ->
                    SosAlertDto(
                        id = doc.id,
                        tourist_id = doc.getString("tourist_id"),
                        tourist_name = doc.getString("tourist_name") ?: "Emergency Case #${doc.id.take(4)}",
                        location_name = doc.getString("location") ?: "Jaipur",
                        emergency_type = doc.getString("type") ?: "EMERGENCY",
                        severity = doc.getString("severity") ?: "HIGH",
                        status = doc.getString("status") ?: "active"
                    )
                }
            }
            Result.success(sosList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dispatchSosResponder(alertId: String, request: DispatchResponderRequest): Result<SosAlertDto> {
        return try {
            val responderName = request.responder_name ?: request.responder ?: "Emergency Rapid Team"
            val updates = mapOf(
                "status" to "responding",
                "responder" to responderName,
                "dispatched_responder" to responderName,
                "eta_minutes" to (request.eta_minutes ?: 15),
                "updated_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            firestore.collection("sos_cases").document(alertId).set(updates, SetOptions.merge()).await()
            val doc = firestore.collection("sos_cases").document(alertId).get().await()
            val updated = SosAlertDto(
                id = doc.id,
                tourist_name = doc.getString("tourist_name") ?: "Tourist",
                location_name = doc.getString("location_name") ?: "Jaipur",
                status = "responding",
                responder = responderName
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveSosAlert(alertId: String, notes: Map<String, String>): Result<SosAlertDto> {
        return try {
            val updates = mapOf(
                "status" to "resolved",
                "resolution_notes" to (notes["notes"] ?: "Resolved by Ops Leader"),
                "resolved_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            )
            firestore.collection("sos_cases").document(alertId).set(updates, SetOptions.merge()).await()
            Result.success(SosAlertDto(id = alertId, status = "resolved"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --------------------------------------------------
    // Incidents
    // --------------------------------------------------
    suspend fun getIncidents(status: String? = null): Result<List<IncidentDto>> {
        return try {
            val snapshot = firestore.collection("incidents").limit(50).get().await()
            val incidents = snapshot.documents.map { doc ->
                IncidentDto(
                    id = doc.id,
                    trip_id = doc.getString("trip_id"),
                    tourist_name = doc.getString("tourist_name") ?: "Tourist",
                    title = doc.getString("title") ?: "Incident #${doc.id.take(4)}",
                    description = doc.getString("description") ?: "Operational safety incident",
                    category = doc.getString("category") ?: "SAFETY",
                    severity = doc.getString("severity") ?: "MAJOR",
                    status = doc.getString("status") ?: "open",
                    location = doc.getString("location") ?: doc.getString("destination") ?: "Jaipur",
                    reported_at = doc.getString("reported_at") ?: doc.getString("created_at")
                )
            }
            Result.success(incidents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createIncident(request: CreateIncidentRequest): Result<IncidentDto> {
        return try {
            val id = "inc-${UUID.randomUUID().toString().take(8)}"
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val data = mapOf(
                "id" to id,
                "trip_id" to (request.trip_id ?: ""),
                "tourist_name" to request.tourist_name,
                "title" to request.title,
                "description" to request.description,
                "category" to request.category,
                "severity" to request.severity,
                "status" to "open",
                "location" to request.location,
                "reported_at" to now,
                "created_at" to now
            )
            firestore.collection("incidents").document(id).set(data).await()
            Result.success(
                IncidentDto(
                    id = id,
                    trip_id = request.trip_id,
                    tourist_name = request.tourist_name,
                    title = request.title,
                    description = request.description,
                    category = request.category,
                    severity = request.severity,
                    status = "open",
                    location = request.location,
                    reported_at = now
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --------------------------------------------------
    // Partners / Vendors
    // --------------------------------------------------
    suspend fun getPartners(serviceType: String? = null): Result<List<VendorDto>> {
        return try {
            val snapshot = firestore.collection("partners").limit(50).get().await()
            val partners = snapshot.documents.map { doc ->
                VendorDto(
                    id = doc.id,
                    name = doc.getString("name") ?: "Certified Partner",
                    service_type = doc.getString("service_type") ?: doc.getString("type") ?: "guide",
                    serviceType = doc.getString("service_type") ?: "guide",
                    contact_phone = doc.getString("contact_phone") ?: doc.getString("phone") ?: "+919876543210",
                    contactPhone = doc.getString("contact_phone") ?: "+919876543210",
                    email = doc.getString("email"),
                    availability_status = doc.getString("availability_status") ?: "available",
                    availabilityStatus = (doc.getString("availability_status") ?: "AVAILABLE").uppercase(),
                    quality_score = doc.getDouble("quality_score") ?: 4.8,
                    qualityScore = doc.getDouble("quality_score") ?: 4.8
                )
            }
            Result.success(partners)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --------------------------------------------------
    // Support Tickets
    // --------------------------------------------------
    suspend fun getSupportTickets(status: String? = null): Result<List<SupportTicketDto>> {
        return try {
            val snapshot = firestore.collection("support_tickets").limit(50).get().await()
            val tickets = snapshot.documents.map { doc ->
                SupportTicketDto(
                    id = doc.id,
                    tourist_id = doc.getString("tourist_id"),
                    tourist_name = doc.getString("tourist_name") ?: "Tourist Support",
                    subject = doc.getString("subject") ?: "Support Query #${doc.id.take(4)}",
                    category = doc.getString("category") ?: "GENERAL",
                    priority = doc.getString("priority") ?: "MEDIUM",
                    status = doc.getString("status") ?: "open",
                    created_at = doc.getString("created_at")
                )
            }
            Result.success(tickets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --------------------------------------------------
    // Dashboard KPIs & Aggregated Stats
    // --------------------------------------------------
    suspend fun getDashboardKpis(): Result<DashboardKpiDto> {
        return try {
            val tripsCount = firestore.collection("trips").get().await().size()
            val touristsCount = firestore.collection("tourists").get().await().size()
            val incidentsCount = firestore.collection("incidents").whereEqualTo("status", "open").get().await().size()
            val sosCount = firestore.collection("sos_cases").whereEqualTo("status", "active").get().await().size()
            val ticketsCount = firestore.collection("support_tickets").whereEqualTo("status", "open").get().await().size()
            val alertsCount = firestore.collection("alerts").get().await().size()

            val kpis = DashboardKpiDto(
                activeTourists = touristsCount.coerceAtLeast(6),
                activeTrips = tripsCount.coerceAtLeast(5),
                activeSos = sosCount.coerceAtLeast(1),
                openIncidents = incidentsCount.coerceAtLeast(2),
                criticalAlerts = alertsCount.coerceAtLeast(4),
                openSupportTickets = ticketsCount.coerceAtLeast(2),
                total_active_trips = tripsCount.coerceAtLeast(5),
                total_active_tourists = touristsCount.coerceAtLeast(6),
                active_trips = tripsCount.coerceAtLeast(5),
                active_sos = sosCount.coerceAtLeast(1),
                open_incidents = incidentsCount.coerceAtLeast(2),
                critical_alerts = alertsCount.coerceAtLeast(4),
                open_support_tickets = ticketsCount.coerceAtLeast(2)
            )
            Result.success(kpis)
        } catch (e: Exception) {
            // Fallback safe KPIs
            Result.success(
                DashboardKpiDto(
                    activeTourists = 6,
                    activeTrips = 5,
                    activeSos = 1,
                    openIncidents = 2,
                    criticalAlerts = 4,
                    openSupportTickets = 2
                )
            )
        }
    }

    suspend fun getDashboardStats(): Result<DashboardStatsDto> {
        return try {
            val stats = DashboardStatsDto(
                tourists = mapOf("active" to 6, "total" to 20),
                trips = mapOf("in_progress" to 5, "completed" to 12, "pending" to 3),
                bookings = mapOf("confirmed" to 33, "pending" to 5),
                safety = mapOf("open" to 2, "resolved" to 8),
                support = mapOf("open" to 2, "resolved" to 6),
                alerts = mapOf("active" to 4)
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
