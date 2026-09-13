package com.travellikepro.opsleader

import com.travellikepro.opsleader.data.api.*
import com.travellikepro.opsleader.data.local.datastore.SessionManager
import com.travellikepro.opsleader.data.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class FakeApiService : ApiService {
    var shouldFail = false
    var loggedInUser = UserResponse(id = "user-123", name = "Chief Leader", email = "leader@test.com", role = "OPS_LEADER", region = "North")

    override suspend fun login(request: LoginRequest): Response<ApiResponse<AuthResponse>> {
        if (shouldFail) {
            return Response.error(401, "{\"message\":\"Invalid credentials\"}".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        val auth = AuthResponse(success = true, token = "test-jwt-token", user = loggedInUser)
        return Response.success(ApiResponse(success = true, data = auth, user = loggedInUser, token = "test-jwt-token"))
    }

    override suspend fun googleAuth(request: GoogleAuthRequest): Response<AuthResponse> {
        if (shouldFail) {
            return Response.error(400, "{\"message\":\"Invalid Google token\"}".toResponseBody("application/json".toMediaTypeOrNull()))
        }
        return Response.success(AuthResponse(success = true, token = "google-jwt-token", user = loggedInUser))
    }

    override suspend fun signup(request: SignupRequest): Response<SignupResponse> {
        if (request.referenceId != "1326") {
            return Response.success(SignupResponse(success = false, message = "Invalid reference ID", errorCode = "INVALID_REFERENCE", user = null))
        }
        return Response.success(SignupResponse(success = true, message = "Signup registered", errorCode = null, user = loggedInUser))
    }

    override suspend fun getMe(): Response<ApiResponse<UserResponse>> {
        if (shouldFail) return Response.error(401, "".toResponseBody(null))
        return Response.success(ApiResponse(success = true, data = loggedInUser))
    }

    override suspend fun logout(): Response<ApiResponse<Unit>> {
        return Response.success(ApiResponse(success = true, data = Unit))
    }

    override suspend fun getDashboardStats(): Response<ApiResponse<DashboardStatsDto>> {
        return Response.success(ApiResponse(success = true, data = DashboardStatsDto()))
    }

    override suspend fun getDashboardKpis(): Response<ApiResponse<DashboardKpiDto>> {
        return Response.success(ApiResponse(success = true, data = DashboardKpiDto(active_trips = 5, active_sos = 1)))
    }

    override suspend fun getChartData(type: String, days: Int): Response<ApiResponse<List<Map<String, Any>>>> {
        return Response.success(ApiResponse(success = true, data = emptyList()))
    }

    override suspend fun getTrips(status: String?): Response<ApiResponse<List<TripDto>>> {
        val list = listOf(
            TripDto(id = "TR-101", destination = "Jaipur Tour", status = "pending"),
            TripDto(id = "TR-102", destination = "Goa Cruise", status = "assigned")
        )
        return Response.success(ApiResponse(success = true, data = if (status != null) list.filter { it.status == status } else list))
    }

    override suspend fun getTripById(tripId: String): Response<ApiResponse<TripDto>> {
        return Response.success(ApiResponse(success = true, data = TripDto(id = tripId, destination = "Jaipur Tour", status = "pending")))
    }

    override suspend fun assignTrip(tripId: String, request: AssignTripRequest): Response<ApiResponse<TripDto>> {
        return Response.success(ApiResponse(success = true, data = TripDto(id = tripId, destination = "Jaipur Tour", status = "assigned", assigned_vendor_name = request.vendor_name)))
    }

    override suspend fun addTripNote(tripId: String, request: AddNoteRequest): Response<ApiResponse<Any>> {
        return Response.success(ApiResponse(success = true, data = Any()))
    }

    override suspend fun getPartners(serviceType: String?): Response<ApiResponse<List<VendorDto>>> {
        val list = listOf(
            VendorDto(id = "VEN-1", name = "Heritage Guides", service_type = "guide", availability_status = "available"),
            VendorDto(id = "VEN-2", name = "Royal Cabs", service_type = "transport", availability_status = "available")
        )
        return Response.success(ApiResponse(success = true, data = if (serviceType != null) list.filter { it.service_type == serviceType } else list))
    }

    override suspend fun getPartnerById(partnerId: String): Response<ApiResponse<VendorDto>> {
        return Response.success(ApiResponse(success = true, data = VendorDto(id = partnerId, name = "Heritage Guides")))
    }

    override suspend fun getTeam(): Response<ApiResponse<List<TeamMemberDto>>> {
        return Response.success(ApiResponse(success = true, data = listOf(TeamMemberDto(id = "TM-1", name = "Alice", email = "alice@test.com", role = "OPS"))))
    }

    override suspend fun getBookings(status: String?): Response<ApiResponse<List<BookingDto>>> {
        return Response.success(ApiResponse(success = true, data = listOf(BookingDto(id = "BK-1", tourist_name = "John", booking_status = "confirmed"))))
    }

    override suspend fun getBookingById(bookingId: String): Response<ApiResponse<BookingDto>> {
        return Response.success(ApiResponse(success = true, data = BookingDto(id = bookingId, tourist_name = "John", booking_status = "confirmed")))
    }

    override suspend fun getNotifications(): Response<ApiResponse<List<NotificationDto>>> {
        return Response.success(ApiResponse(success = true, data = listOf(NotificationDto(id = "N-1", title = "Alert", message = "System update"))))
    }

    override suspend fun broadcastNotification(request: BroadcastNotificationRequest): Response<ApiResponse<Unit>> {
        return Response.success(ApiResponse(success = true, data = Unit))
    }

    override suspend fun getSupportTickets(status: String?): Response<ApiResponse<List<SupportTicketDto>>> {
        return Response.success(ApiResponse(success = true, data = listOf(SupportTicketDto(id = "TCK-1", subject = "Query", status = "open"))))
    }

    override suspend fun getSupportTicketById(ticketId: String): Response<ApiResponse<SupportTicketDto>> {
        return Response.success(ApiResponse(success = true, data = SupportTicketDto(id = ticketId, subject = "Query", status = "open")))
    }

    override suspend fun sendTicketMessage(ticketId: String, request: SendTicketMessageRequest): Response<ApiResponse<SupportTicketDto>> {
        return Response.success(ApiResponse(success = true, data = SupportTicketDto(id = ticketId, subject = "Query", status = "in_progress")))
    }

    override suspend fun getSosAlerts(status: String?): Response<ApiResponse<List<SosAlertDto>>> {
        return Response.success(ApiResponse(success = true, data = listOf(SosAlertDto(id = "SOS-1", emergency_type = "MEDICAL", status = "active"))))
    }

    override suspend fun dispatchSosResponder(alertId: String, request: DispatchResponderRequest): Response<ApiResponse<SosAlertDto>> {
        return Response.success(ApiResponse(success = true, data = SosAlertDto(id = alertId, emergency_type = "MEDICAL", status = "responder_dispatched", dispatched_responder = request.responder_name)))
    }

    override suspend fun resolveSosAlert(alertId: String, notes: Map<String, String>): Response<ApiResponse<SosAlertDto>> {
        return Response.success(ApiResponse(success = true, data = SosAlertDto(id = alertId, emergency_type = "MEDICAL", status = "resolved")))
    }

    override suspend fun getIncidents(status: String?): Response<ApiResponse<List<IncidentDto>>> {
        return Response.success(ApiResponse(success = true, data = listOf(IncidentDto(id = "INC-1", title = "Luggage delay", description = "Minor delay", location = "Jaipur"))))
    }

    override suspend fun createIncident(request: CreateIncidentRequest): Response<ApiResponse<IncidentDto>> {
        return Response.success(ApiResponse(success = true, data = IncidentDto(id = "INC-2", title = request.title, description = request.description, location = request.location)))
    }

    override suspend fun getIncidentById(incidentId: String): Response<ApiResponse<IncidentDto>> {
        return Response.success(ApiResponse(success = true, data = IncidentDto(id = incidentId, title = "Luggage delay", description = "Minor delay", location = "Jaipur")))
    }

    override suspend fun updateIncidentStatus(incidentId: String, body: Map<String, String>): Response<ApiResponse<IncidentDto>> {
        return Response.success(ApiResponse(success = true, data = IncidentDto(id = incidentId, title = "Luggage delay", description = "Minor delay", location = "Jaipur", status = body["status"] ?: "investigating")))
    }

    override suspend fun resolveIncident(incidentId: String, body: Map<String, String>): Response<ApiResponse<IncidentDto>> {
        return Response.success(ApiResponse(success = true, data = IncidentDto(id = incidentId, title = "Luggage delay", description = "Minor delay", location = "Jaipur", status = "resolved", resolution_summary = body["resolution_summary"])))
    }

    override suspend fun getSafetyChecklists(): Response<ApiResponse<List<Map<String, Any>>>> {
        return Response.success(ApiResponse(success = true, data = emptyList()))
    }

    override suspend fun getWeatherAlerts(): Response<ApiResponse<List<Map<String, Any>>>> {
        return Response.success(ApiResponse(success = true, data = emptyList()))
    }

    override suspend fun getTourists(): Response<ApiResponse<List<TouristDto>>> {
        return Response.success(ApiResponse(success = true, data = listOf(TouristDto(id = "T-1", name = "Sarah"))))
    }

    override suspend fun getTouristById(touristId: String): Response<ApiResponse<TouristDto>> {
        return Response.success(ApiResponse(success = true, data = TouristDto(id = touristId, name = "Sarah")))
    }
}

class FakeSessionManager : SessionManager() {
    private var token: String? = null
    private var userId: String? = null
    private var userName: String? = null
    private var userEmail: String? = null
    private var userRole: String? = null

    override fun saveTokens(accessToken: String, refreshToken: String?) {
        this.token = accessToken
    }

    override fun getAccessToken(): String? = token

    override fun saveUser(id: String?, name: String?, email: String?, role: String?, phone: String?, region: String?) {
        this.userId = id
        this.userName = name
        this.userEmail = email
        this.userRole = role
    }

    override fun getUserId(): String? = userId
    override fun getUserName(): String? = userName
    override fun getUserEmail(): String? = userEmail
    override fun getUserRole(): String? = userRole
    override fun isLoggedIn(): Boolean = !token.isNullOrBlank()

    override fun clearSession() {
        token = null
        userId = null
        userName = null
        userEmail = null
        userRole = null
    }
}

class AppUnitTests {

    private lateinit var apiService: FakeApiService
    private lateinit var sessionManager: FakeSessionManager
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        apiService = FakeApiService()
        sessionManager = FakeSessionManager()
        authRepository = AuthRepository(apiService, sessionManager)
    }

    @Test
    fun testLoginSuccess() = runTest {
        val result = authRepository.login("leader@test.com", "Password@123")
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("Chief Leader", user?.name)
        assertTrue(sessionManager.isLoggedIn())
        assertEquals("test-jwt-token", sessionManager.getAccessToken())
    }

    @Test
    fun testLoginFailure() = runTest {
        apiService.shouldFail = true
        val result = authRepository.login("leader@test.com", "wrong")
        assertTrue(result.isFailure)
        assertFalse(sessionManager.isLoggedIn())
    }

    @Test
    fun testSignupValidation() = runTest {
        val successResponse = authRepository.signup(SignupRequest(name = "Alice", email = "alice@test.com", referenceId = "1326"))
        assertTrue(successResponse.isSuccessful)
        assertTrue(successResponse.body()?.success == true)

        val invalidResponse = authRepository.signup(SignupRequest(name = "Alice", email = "alice@test.com", referenceId = "9999"))
        assertTrue(invalidResponse.isSuccessful)
        assertFalse(invalidResponse.body()?.success == true)
    }

    @Test
    fun testLogoutClearsSession() = runTest {
        sessionManager.saveTokens("valid-token")
        assertTrue(sessionManager.isLoggedIn())

        authRepository.logout()
        assertFalse(sessionManager.isLoggedIn())
        assertNull(sessionManager.getAccessToken())
    }

    @Test
    fun testTripsAndAssignment() = runTest {
        val opsRepo = com.travellikepro.opsleader.data.repository.OperationsRepository(apiService)
        val tripsResult = opsRepo.getTrips()
        assertTrue(tripsResult.isSuccess)
        assertEquals(2, tripsResult.getOrNull()?.size)

        val assignResult = opsRepo.assignTrip("TR-101", AssignTripRequest(vendor_id = "VEN-1", vendor_name = "Heritage Guides"))
        assertTrue(assignResult.isSuccess)
        assertEquals("Heritage Guides", assignResult.getOrNull()?.assigned_vendor_name)
    }

    @Test
    fun testSafetySosDispatchAndResolve() = runTest {
        val safetyRepo = com.travellikepro.opsleader.data.repository.SafetyRepository(apiService)
        val sosResult = safetyRepo.getSosAlerts("active")
        assertTrue(sosResult.isSuccess)
        assertEquals(1, sosResult.getOrNull()?.size)

        val dispatchResult = safetyRepo.dispatchResponder("SOS-1", DispatchResponderRequest(responder_name = "Jaipur Team 1", eta_minutes = 10))
        assertTrue(dispatchResult.isSuccess)
        assertEquals("Jaipur Team 1", dispatchResult.getOrNull()?.dispatched_responder)

        val resolveResult = safetyRepo.resolveSosAlert("SOS-1")
        assertTrue(resolveResult.isSuccess)
        assertEquals("resolved", resolveResult.getOrNull()?.status)
    }

    @Test
    fun testSupportTicketsAndBroadcast() = runTest {
        val supportRepo = com.travellikepro.opsleader.data.repository.SupportRepository(apiService)
        val ticketsResult = supportRepo.getSupportTickets()
        assertTrue(ticketsResult.isSuccess)
        assertEquals(1, ticketsResult.getOrNull()?.size)

        val msgResult = supportRepo.sendTicketMessage("TCK-1", "Help is on the way")
        assertTrue(msgResult.isSuccess)

        val broadcastResult = supportRepo.broadcastNotification(BroadcastNotificationRequest(title = "Alert", body = "Weather storm"))
        assertTrue(broadcastResult.isSuccess)
    }

    @Test
    fun testTouristsDirectory() = runTest {
        val touristRepo = com.travellikepro.opsleader.data.repository.TouristRepository(apiService)
        val touristsResult = touristRepo.getTourists()
        assertTrue(touristsResult.isSuccess)
        assertEquals(1, touristsResult.getOrNull()?.size)
        assertEquals("Sarah", touristsResult.getOrNull()?.first()?.name)
    }
}
