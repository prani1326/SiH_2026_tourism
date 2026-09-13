package com.touristapp.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.touristapp.data.remote.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class BackendHealth(
    val isReachable: Boolean,
    val status: String,
    val serviceName: String? = null,
    val version: String? = null,
    val firebaseProject: String? = null,
    val errorMessage: String? = null
)

/**
 * Resilient, commercial-grade HTTP client connecting Android frontend to FastAPI backend.
 * Provides typed methods for all platform services with automatic Firebase Auth bearer token injection.
 */
class BackendApiClient(
    baseUrl: String = DEFAULT_BASE_URL,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        // 10.0.2.2 for Android Emulator, 10.178.117.4 for Wi-Fi LAN, 127.0.0.1/localhost for ADB reverse
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000"
        val CANDIDATE_HOSTS = listOf(
            "http://10.0.2.2:8000",
            "http://10.178.117.4:8000",
            "http://127.0.0.1:8000",
            "http://localhost:8000",
            "http://172.20.10.8:8000"
        )
    }

    private var activeBaseUrl: String = baseUrl
    private var lastLocalFailureTime: Long = 0L

    val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private suspend fun getAuthHeader(): String? {
        return try {
            val user = auth.currentUser
            val tokenResult = user?.getIdToken(false)?.await()
            tokenResult?.token?.let { "Bearer $it" }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun executeRaw(
        method: String,
        endpointPath: String,
        bodyJson: String? = null,
        requiresAuth: Boolean = false,
        timeoutMs: Int = 3000
    ): Result<String> = withContext(Dispatchers.IO) {
        val authHeader = if (requiresAuth) getAuthHeader() else null

        if (endpointPath.startsWith("http")) {
            return@withContext executeSingleRaw(method, endpointPath, bodyJson, authHeader, timeoutMs)
        }

        val now = System.currentTimeMillis()
        // If local hosts failed recently (within 20s), fail fast to allow instantaneous Firestore direct access
        if (now - lastLocalFailureTime < 20_000L && activeBaseUrl.contains("172.20.") || activeBaseUrl.contains("10.0.") || activeBaseUrl.contains("localhost")) {
            val quickAttempt = executeSingleRaw(method, "$activeBaseUrl$endpointPath", bodyJson, authHeader, timeoutMs = 800)
            if (quickAttempt.isSuccess) {
                lastLocalFailureTime = 0L
                return@withContext quickAttempt
            }
            return@withContext quickAttempt
        }

        // Try activeBaseUrl first with fast connect timeout
        val firstAttempt = executeSingleRaw(method, "$activeBaseUrl$endpointPath", bodyJson, authHeader, timeoutMs)
        if (firstAttempt.isSuccess) {
            lastLocalFailureTime = 0L
            return@withContext firstAttempt
        }

        // If network connectivity error, try candidate fallback hosts with fast timeout (600ms)
        val firstEx = firstAttempt.exceptionOrNull()
        if (firstEx is java.net.ConnectException || firstEx is java.net.SocketTimeoutException || firstEx?.message?.contains("Failed to connect") == true || firstEx?.message?.contains("failed to connect") == true) {
            lastLocalFailureTime = now
            for (candidate in CANDIDATE_HOSTS) {
                if (candidate == activeBaseUrl) continue
                val fallbackRes = executeSingleRaw(method, "$candidate$endpointPath", bodyJson, authHeader, timeoutMs = 600)
                if (fallbackRes.isSuccess) {
                    activeBaseUrl = candidate
                    lastLocalFailureTime = 0L
                    return@withContext fallbackRes
                }
            }
        }

        firstAttempt
    }

    private fun executeSingleRaw(
        method: String,
        fullUrl: String,
        bodyJson: String?,
        authHeader: String?,
        timeoutMs: Int
    ): Result<String> {
        var connection: HttpURLConnection? = null
        return try {
            Log.d("BackendApiClient", "--> [$method] $fullUrl")
            var currentUrl = fullUrl
            var currentConn: HttpURLConnection? = null
            var responseCode = -1
            var redirects = 0
            val maxRedirects = 3

            while (redirects <= maxRedirects) {
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = if (timeoutMs > 2000) 1500 else timeoutMs
                    readTimeout = timeoutMs
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Connection", "Keep-Alive")
                    setRequestProperty("Keep-Alive", "timeout=30, max=100")
                    setRequestProperty("Accept-Encoding", "gzip, deflate")
                    if (bodyJson != null) {
                        setRequestProperty("Content-Type", "application/json")
                    }
                    if (authHeader != null) {
                        setRequestProperty("Authorization", authHeader)
                    }
                }

                if (bodyJson != null) {
                    conn.doOutput = true
                    OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                        writer.write(bodyJson)
                        writer.flush()
                    }
                }

                connection = conn
                currentConn = conn
                responseCode = conn.responseCode

                if ((responseCode == 301 || responseCode == 302 || responseCode == 307 || responseCode == 308) && redirects < maxRedirects) {
                    val location = conn.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        redirects++
                        conn.disconnect()
                        currentUrl = if (location.startsWith("http")) location else "$activeBaseUrl$location"
                        continue
                    }
                }
                break
            }

            val conn = currentConn ?: throw IllegalStateException("No connection established")
            val rawInputStream = if (responseCode in 200..299) {
                conn.inputStream
            } else {
                conn.errorStream ?: conn.inputStream
            }

            val inputStream = if ("gzip".equals(conn.contentEncoding, ignoreCase = true)) {
                java.util.zip.GZIPInputStream(rawInputStream)
            } else {
                rawInputStream
            }

            val responseText = BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { it.readText() }
            Log.d("BackendApiClient", "<-- [$method] $fullUrl HTTP $responseCode")

            if (responseCode in 200..299) {
                Result.success(responseText)
            } else {
                Result.failure(Exception("HTTP $responseCode: $responseText"))
            }
        } catch (e: Exception) {
            Log.w("BackendApiClient", "xx- [$method] $fullUrl failed: ${e.message}")
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    // --- Health Check ---
    suspend fun checkHealth(): BackendHealth = withContext(Dispatchers.IO) {
        try {
            val res = executeRaw("GET", "/health", timeoutMs = 1500)
            if (res.isSuccess) {
                val json = JSONObject(res.getOrThrow())
                BackendHealth(
                    isReachable = true,
                    status = json.optString("status", "healthy"),
                    serviceName = json.optString("service", "Tourist Platform API"),
                    version = json.optString("version", "2.0.0"),
                    firebaseProject = json.optString("firebase_project", "trip-planner-version-1")
                )
            } else {
                BackendHealth(
                    isReachable = false,
                    status = "offline",
                    errorMessage = res.exceptionOrNull()?.message
                )
            }
        } catch (e: Exception) {
            BackendHealth(
                isReachable = false,
                status = "offline",
                errorMessage = e.message
            )
        }
    }

    // --- Destinations (/api/v1/destinations) ---
    suspend fun getPopularDestinations(): Result<List<com.touristapp.data.models.DestinationDto>> {
        val res = executeRaw("GET", "/api/v1/destinations/popular", timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getDestinations(tag: String? = null, query: String? = null): Result<List<com.touristapp.data.models.DestinationDto>> {
        val params = mutableListOf<String>()
        if (!tag.isNullOrBlank()) params.add("tag=" + java.net.URLEncoder.encode(tag, "UTF-8"))
        if (!query.isNullOrBlank()) params.add("query=" + java.net.URLEncoder.encode(query, "UTF-8"))
        val queryString = if (params.isNotEmpty()) "?" + params.joinToString("&") else ""
        val res = executeRaw("GET", "/api/v1/destinations$queryString", timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getDestinationById(id: String): Result<com.touristapp.data.models.DestinationDto> {
        val encodedId = java.net.URLEncoder.encode(id, "UTF-8")
        val res = executeRaw("GET", "/api/v1/destinations/$encodedId", timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun searchDestinations(query: String): Result<List<com.touristapp.data.models.DestinationDto>> {
        val encodedQ = java.net.URLEncoder.encode(query, "UTF-8")
        val res = executeRaw("GET", "/api/v1/destinations?query=$encodedQ", timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    // --- AI Trip Planner (/api/v1/trips/ai-plan) ---
    suspend fun generateAiPlan(request: AITripPlanRequestDto): Result<GeneratedItineraryPlanDto> {
        val body = jsonParser.encodeToString(request)
        val res = executeRaw("POST", "/api/v1/trips/ai-plan", bodyJson = body, requiresAuth = true, timeoutMs = 25000)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    // --- Community (/api/v1/community) ---
    suspend fun getCommunityForums(): Result<List<CommunityForumDto>> {
        val res = executeRaw("GET", "/api/v1/community/forums")
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getCommunityPosts(forumId: String? = null): Result<List<CommunityPostDto>> {
        val path = if (forumId != null) "/api/v1/community/posts?forum_id=$forumId" else "/api/v1/community/posts"
        val res = executeRaw("GET", path)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun createCommunityPost(forumId: String, title: String, content: String): Result<CommunityPostDto> {
        val json = JSONObject().apply {
            put("forum_id", forumId)
            put("title", title)
            put("content", content)
        }.toString()
        val res = executeRaw("POST", "/api/v1/community/posts", bodyJson = json, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getCreatorItineraries(): Result<List<CreatorItineraryDto>> {
        val res = executeRaw("GET", "/api/v1/community/creators/itineraries")
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun copyCreatorItinerary(creatorItineraryId: String): Result<String> {
        val res = executeRaw("POST", "/api/v1/community/creators/itineraries/$creatorItineraryId/copy", requiresAuth = true)
        return res.mapCatching {
            val json = JSONObject(it)
            json.optString("trip_id", creatorItineraryId)
        }
    }

    // --- Bookings & Payments (/api/v1/bookings & /api/v1/payments) ---
    suspend fun requestBooking(request: RequestToBookDto): Result<BookingOutDto> {
        val body = jsonParser.encodeToString(request)
        val res = executeRaw("POST", "/api/v1/bookings/request", bodyJson = body, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getUserBookings(): Result<List<BookingOutDto>> {
        val res = executeRaw("GET", "/api/v1/bookings/", requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun createPaymentOrder(bookingId: String): Result<RazorpayOrderOutDto> {
        val body = jsonParser.encodeToString(RazorpayOrderCreateDto(bookingId = bookingId))
        val res = executeRaw("POST", "/api/v1/payments/order", bodyJson = body, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun verifyPayment(verifyReq: RazorpayVerifyDto): Result<Boolean> {
        val body = jsonParser.encodeToString(verifyReq)
        val res = executeRaw("POST", "/api/v1/payments/verify", bodyJson = body, requiresAuth = true)
        return res.mapCatching {
            val json = JSONObject(it)
            json.optBoolean("success", true)
        }
    }

    suspend fun getInvoice(bookingId: String): Result<DigitalInvoiceDto> {
        val res = executeRaw("GET", "/api/v1/payments/$bookingId/invoice", requiresAuth = false)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun processDirectPayment(request: PaymentProcessRequestDto): Result<PaymentReceiptOutDto> {
        val body = jsonParser.encodeToString(request)
        val res = executeRaw("POST", "/api/v1/payments/process", bodyJson = body, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun cancelBooking(bookingId: String, reason: String = "User requested cancellation"): Result<RefundStatusDto> {
        val req = CancellationRequestDto(bookingId = bookingId, cancellationReason = reason)
        val body = jsonParser.encodeToString(req)
        val res = executeRaw("POST", "/api/v1/refunds/cancel", bodyJson = body, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getRefundStatus(bookingId: String): Result<RefundStatusDto> {
        val res = executeRaw("GET", "/api/v1/refunds/$bookingId/status", requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    // --- Safety & SOS (/api/v1/safety) ---
    suspend fun triggerSos(request: SOSAlertRequestDto): Result<SOSAlertResponseDto> {
        val body = jsonParser.encodeToString(request)
        val res = executeRaw("POST", "/api/v1/safety/sos", bodyJson = body, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun resolveSos(alertId: String): Result<Boolean> {
        val res = executeRaw("POST", "/api/v1/safety/sos/$alertId/resolve", requiresAuth = true)
        return res.mapCatching {
            val json = JSONObject(it)
            json.optBoolean("success", true)
        }
    }

    suspend fun sendHeartbeat(request: DeviceHeartbeatRequestDto): Result<HeartbeatResponseDto> {
        val body = jsonParser.encodeToString(request)
        val res = executeRaw("POST", "/api/v1/safety/heartbeat", bodyJson = body, requiresAuth = true, timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun reportImSafe(alertId: String? = null): Result<SafetyActionResponseDto> {
        val req = ImSafeRequestDto(alertId = alertId)
        val body = jsonParser.encodeToString(req)
        val res = executeRaw("POST", "/api/v1/safety/im-safe", bodyJson = body, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun reportNeedHelp(tripId: String? = null, details: String = "Traveler requested help", lat: Double? = null, lon: Double? = null): Result<SafetyActionResponseDto> {
        val req = NeedHelpRequestDto(tripId = tripId, details = details, latitude = lat, longitude = lon)
        val body = jsonParser.encodeToString(req)
        val res = executeRaw("POST", "/api/v1/safety/need-help", bodyJson = body, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun recoverLostPhone(email: String, pin: String): Result<LostPhoneRecoverResponseDto> {
        val body = jsonParser.encodeToString(LostPhoneRecoverDto(email = email, emergencyRecoveryPin = pin))
        val res = executeRaw("POST", "/api/v1/lost-phone/recover", bodyJson = body)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    // --- Support (/api/v1/support) ---
    suspend fun getUserTickets(): Result<List<SupportTicketOutDto>> {
        val res = executeRaw("GET", "/api/v1/support/tickets", requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun createTicket(ticket: SupportTicketCreateDto): Result<SupportTicketOutDto> {
        val body = jsonParser.encodeToString(ticket)
        val res = executeRaw("POST", "/api/v1/support/tickets", bodyJson = body, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    // --- Travel Tools: SIM, Visa, Food Scan, Translation ---
    suspend fun getSimPlans(): Result<List<SimPlanDto>> {
        val res = executeRaw("GET", "/api/v1/sim-esim/plans")
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun purchaseSimPlan(planId: String): Result<String> {
        val res = executeRaw("POST", "/api/v1/sim-esim/purchase/$planId", requiresAuth = true)
        return res.mapCatching {
            val json = JSONObject(it)
            json.optString("activation_code", "ACT-SUCCESS")
        }
    }

    suspend fun getVisaDocuments(): Result<List<VisaDocumentDto>> {
        val res = executeRaw("GET", "/api/v1/visa-vault/")
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun translateText(text: String, targetLang: String, sourceLang: String = "en"): Result<TranslationResponseDto> {
        val req = TranslationRequestDto(text = text, targetLanguage = targetLang, sourceLanguage = sourceLang)
        val body = jsonParser.encodeToString(req)
        val res = executeRaw("POST", "/api/v1/translation/text", bodyJson = body)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun scanMenu(menuText: String, diet: String): Result<MenuScanResponseDto> {
        val req = MenuScanRequestDto(menuText = menuText, userDiet = diet)
        val body = jsonParser.encodeToString(req)
        val res = executeRaw("POST", "/api/v1/food-culture/menu-scan", bodyJson = body)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    // --- Profile & KYC Onboarding (/api/v1/users) ---
    suspend fun checkTripEligibility(): Result<com.touristapp.data.models.TripEligibilityResponse> {
        val res = executeRaw("GET", "/api/v1/users/trip-eligibility", requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun saveProfileOnboardingStep(step: Int, data: Map<String, String>): Result<com.touristapp.data.models.ProfileKycActionResponse> {
        val payload = com.touristapp.data.models.ProfileStepPayload(step = step, data = data)
        val body = jsonParser.encodeToString(payload)
        val res = executeRaw("POST", "/api/v1/users/onboarding/profile-step", bodyJson = body, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun submitKyc(documentType: String, documentNumber: String, expiryDate: String? = null): Result<com.touristapp.data.models.ProfileKycActionResponse> {
        val payload = com.touristapp.data.models.KycSubmitPayload(
            document_type = documentType,
            document_number = documentNumber,
            expiry_date = expiryDate
        )
        val body = jsonParser.encodeToString(payload)
        val res = executeRaw("POST", "/api/v1/users/kyc/submit", bodyJson = body, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun verifyKyc(documentType: String = "Passport", documentNumber: String = "P12345678"): Result<com.touristapp.data.models.ProfileKycActionResponse> {
        val payload = com.touristapp.data.models.KycVerifyPayload(
            document_type = documentType,
            document_number = documentNumber
        )
        val body = jsonParser.encodeToString(payload)
        val res = executeRaw("POST", "/api/v1/users/kyc/verify", bodyJson = body, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun skipKyc(): Result<com.touristapp.data.models.ProfileKycActionResponse> {
        val res = executeRaw("POST", "/api/v1/users/kyc/skip", requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    // --- SIH AI Intelligence Systems ---

    suspend fun getSafetyIntelligence(destination: String = "Jaipur"): Result<com.touristapp.data.models.SafetyIntelligenceDto> {
        val enc = java.net.URLEncoder.encode(destination, "UTF-8")
        val res = executeRaw("GET", "/api/v1/safety-intel/current?destination=$enc", timeoutMs = 3000)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getSafeRoutes(origin: String = "Hotel / Station", destination: String = "Hawa Mahal, Jaipur"): Result<List<com.touristapp.data.models.SafeRouteDto>> {
        val json = JSONObject().apply {
            put("origin", origin)
            put("destination", destination)
        }.toString()
        val res = executeRaw("POST", "/api/v1/safety-intel/routes", bodyJson = json, timeoutMs = 3000)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getCrowdIntelligence(monumentName: String = "Amber Fort"): Result<com.touristapp.data.models.CrowdIntelligenceDto> {
        val enc = java.net.URLEncoder.encode(monumentName, "UTF-8")
        val res = executeRaw("GET", "/api/v1/crowd/monument?name=$enc", timeoutMs = 3000)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun startGuardianSession(tripName: String = "Jaipur Heritage Tour"): Result<com.touristapp.data.models.GuardianSessionDto> {
        val json = JSONObject().apply {
            put("trip_name", tripName)
        }.toString()
        val res = executeRaw("POST", "/api/v1/guardian/start", bodyJson = json, requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun checkInGuardianSafe(batteryLevel: Int = 74, locationName: String = "Amber Fort, Jaipur"): Result<Boolean> {
        val json = JSONObject().apply {
            put("battery_level", batteryLevel)
            put("location_name", locationName)
        }.toString()
        val res = executeRaw("POST", "/api/v1/guardian/check-in", bodyJson = json, requiresAuth = true)
        return res.mapCatching {
            val obj = JSONObject(it)
            obj.optBoolean("success", true)
        }
    }

    suspend fun toggleGuardianSharing(enable: Boolean): Result<Boolean> {
        val json = JSONObject().apply {
            put("enable", enable)
        }.toString()
        val res = executeRaw("POST", "/api/v1/guardian/sharing-toggle", bodyJson = json, requiresAuth = true)
        return res.mapCatching {
            val obj = JSONObject(it)
            obj.optBoolean("success", true)
        }
    }

    suspend fun getGuardianStatus(): Result<com.touristapp.data.models.GuardianSessionDto> {
        val res = executeRaw("GET", "/api/v1/guardian/status", requiresAuth = true)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getEmergencyBundle(destination: String = "Jaipur"): Result<com.touristapp.data.models.EmergencyBundleDto> {
        val enc = java.net.URLEncoder.encode(destination, "UTF-8")
        val res = executeRaw("GET", "/api/v1/emergency/bundle?destination=$enc", timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun syncOfflineSos(packetId: String, lat: Double, lon: Double, timestamp: String, emergencyType: String = "SOS_OFFLINE", notes: String? = null): Result<Boolean> {
        val json = JSONObject().apply {
            put("packet_id", packetId)
            put("latitude", lat)
            put("longitude", lon)
            put("timestamp_offline", timestamp)
            put("emergency_type", emergencyType)
            if (notes != null) put("medical_notes", notes)
        }.toString()
        val res = executeRaw("POST", "/api/v1/emergency/sync", bodyJson = json, requiresAuth = true)
        return res.mapCatching {
            val obj = JSONObject(it)
            obj.optBoolean("success", true)
        }
    }

    suspend fun analyzeHeritage(monumentHint: String = "Amber Fort", imageBase64: String? = null): Result<com.touristapp.data.models.HeritageAnalysisDto> {
        val json = JSONObject().apply {
            put("monument_hint", monumentHint)
            if (imageBase64 != null) put("image_base64", imageBase64)
        }.toString()
        val res = executeRaw("POST", "/api/v1/heritage/analyze", bodyJson = json, timeoutMs = 5000)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getNearbyScamAlerts(destination: String = "Jaipur"): Result<List<com.touristapp.data.models.ScamAlertDto>> {
        val enc = java.net.URLEncoder.encode(destination, "UTF-8")
        val res = executeRaw("GET", "/api/v1/scams/nearby?destination=$enc", timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun reportScam(destination: String, scamType: String, location: String, description: String, estimatedLoss: Double? = null): Result<Boolean> {
        val json = JSONObject().apply {
            put("destination", destination)
            put("scam_type", scamType)
            put("location", location)
            put("description", description)
            if (estimatedLoss != null) put("estimated_loss_inr", estimatedLoss)
        }.toString()
        val res = executeRaw("POST", "/api/v1/scams/report", bodyJson = json, requiresAuth = true)
        return res.mapCatching {
            val obj = JSONObject(it)
            obj.optBoolean("success", true)
        }
    }

    suspend fun getTripSustainability(tripId: String = "trip_demo", transport: String = "Cab"): Result<com.touristapp.data.models.SustainabilityScoreDto> {
        val enc = java.net.URLEncoder.encode(tripId, "UTF-8")
        val res = executeRaw("GET", "/api/v1/sustainability/$enc?transport=$transport", timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getLocalExperiences(destination: String? = "Jaipur"): Result<List<com.touristapp.data.models.LocalExperienceDto>> {
        val path = if (destination != null) "/api/v1/local-experiences?destination=" + java.net.URLEncoder.encode(destination, "UTF-8") else "/api/v1/local-experiences"
        val res = executeRaw("GET", path, timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun getInsurancePolicies(): Result<List<InsurancePolicyDto>> {
        val res = executeRaw("GET", "/api/v1/insurance/policies", timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun purchaseInsurancePolicy(policyId: String): Result<String> {
        val res = executeRaw("POST", "/api/v1/insurance/purchase/$policyId", requiresAuth = true)
        return res.mapCatching {
            val obj = JSONObject(it)
            obj.optString("policy_number", "POL-IND-${(1000..9999).random()}")
        }
    }

    suspend fun getPreparationChecklist(destination: String = "Jaipur"): Result<PreparationChecklistDto> {
        val enc = java.net.URLEncoder.encode(destination, "UTF-8")
        val res = executeRaw("GET", "/api/v1/preparation/$enc", timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }

    suspend fun compareTransport(city: String = "Agra"): Result<TransportComparisonResponseDto> {
        val enc = java.net.URLEncoder.encode(city, "UTF-8")
        val res = executeRaw("GET", "/api/v1/transport-brain/compare?city=$enc", timeoutMs = 2500)
        return res.mapCatching { jsonParser.decodeFromString(it) }
    }
}

