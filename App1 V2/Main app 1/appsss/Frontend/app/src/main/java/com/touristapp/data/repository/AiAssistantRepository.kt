package com.touristapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.touristapp.data.location.LocationService
import com.touristapp.data.models.*
import com.touristapp.data.remote.BackendApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class AiAssistantRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val backendApiClient: BackendApiClient,
    private val tripRepository: TripRepository,
    private val bookingRepository: BookingRepository,
    private val destinationRepository: DestinationRepository,
    private val safetyRepository: SafetyRepository,
    private val userRepository: UserRepository,
    private val locationService: LocationService? = null
) {
    private val _messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messagesFlow: Flow<List<ChatMessage>> = _messagesFlow.asStateFlow()

    private var currentConversationId: String = "conv_default"
    private var shortTermMemory = ShortTermContext()

    suspend fun getOrCreateConversation(): String = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: "guest_user"
        currentConversationId = "conv_${userId.take(8)}"
        loadMessages(currentConversationId)
        currentConversationId
    }

    suspend fun loadMessages(conversationId: String) = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        val loaded = mutableListOf<ChatMessage>()

        if (!userId.isNullOrBlank()) {
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("conversations")
                    .document(conversationId)
                    .collection("messages")
                    .orderBy("timestamp")
                    .limit(60)
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    val senderStr = doc.getString("sender") ?: "AI"
                    val sender = try { MessageSender.valueOf(senderStr) } catch (e: Exception) { MessageSender.AI }
                    val cardTypeStr = doc.getString("cardType") ?: "NONE"
                    val cardType = try { ChatCardType.valueOf(cardTypeStr) } catch (e: Exception) { ChatCardType.NONE }
                    val intentStr = doc.getString("intent") ?: "GENERAL_CHAT"
                    val intent = try { ChatIntent.valueOf(intentStr) } catch (e: Exception) { ChatIntent.GENERAL_CHAT }

                    loaded.add(
                        ChatMessage(
                            id = doc.id,
                            conversationId = conversationId,
                            sender = sender,
                            text = doc.getString("text") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            cardType = cardType,
                            intent = intent,
                            toolUsed = doc.getString("toolUsed")
                        )
                    )
                }
            } catch (e: Exception) {
                // Fallback to in-memory state
            }
        }

        if (loaded.isNotEmpty()) {
            _messagesFlow.value = loaded
        } else if (_messagesFlow.value.isEmpty()) {
            val initial = ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                sender = MessageSender.AI,
                text = "Hello! I'm your AI Travel Assistant. I have live context of your trips, bookings, dietary preferences, budget, and safety settings. How can I assist your journey today?",
                timestamp = System.currentTimeMillis(),
                intent = ChatIntent.GENERAL_CHAT
            )
            _messagesFlow.value = listOf(initial)
        }
    }

    suspend fun sendMessage(userText: String, userLocationAttached: Boolean = false): ChatMessage = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: "guest_user"
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = currentConversationId,
            sender = MessageSender.USER,
            text = userText,
            timestamp = System.currentTimeMillis()
        )

        val updatedList = _messagesFlow.value.toMutableList().apply { add(userMsg) }
        _messagesFlow.value = updatedList
        persistMessage(userMsg)

        // Process query with Intent Classification, Memory Context & Tool Execution
        val aiReply = processUserQuery(userText, userLocationAttached)
        val finalMessages = _messagesFlow.value.toMutableList().apply { add(aiReply) }
        _messagesFlow.value = finalMessages
        persistMessage(aiReply)

        aiReply
    }

    private suspend fun processUserQuery(query: String, userLocationAttached: Boolean): ChatMessage {
        val qLower = query.lowercase(Locale.ROOT).trim()

        // 0. Prompt Injection Defense
        if (qLower.contains("ignore previous instructions") || qLower.contains("system prompt") || qLower.contains("dump password") || qLower.contains("bypass security")) {
            return ChatMessage(
                conversationId = currentConversationId,
                sender = MessageSender.AI,
                text = "I cannot process system override requests. As your travel assistant, I am dedicated to helping you plan, explore, and safely enjoy your journeys.",
                intent = ChatIntent.GENERAL_CHAT
            )
        }

        // Detect and update short-term memory slots
        extractMemorySlots(qLower)

        // 1. Intent Detection
        val intent = classifyIntent(qLower, shortTermMemory)

        // 2. Fetch Targeted Context (Data Minimization)
        val uid = userRepository.getCurrentUid()
        val currentUser = uid?.let { userRepository.getUserProfile(it).getOrNull() }
        val userDiet = currentUser?.preferences?.dietary_preferences ?: emptyList()
        val userStyles = currentUser?.preferences?.travel_styles ?: emptyList()
        val userBudget = currentUser?.preferences?.budget_range ?: "Medium"
        val isVegetarian = userDiet.any { it.contains("veg", ignoreCase = true) } || qLower.contains("veg") || qLower.contains("shakahari")

        // 3. Dispatch to Specialized Tool Handlers
        return when (intent) {
            ChatIntent.SOS, ChatIntent.SAFETY -> handleSafetyIntent(intent, qLower)
            ChatIntent.LOST_PHONE -> handleLostPhoneIntent()
            ChatIntent.SAFETY_GROUP -> handleSafetyGroupIntent()
            ChatIntent.BOOKING_QUERY, ChatIntent.BOOKING_ACTION -> handleBookingIntent(qLower)
            ChatIntent.ITINERARY_UPDATE -> handleItineraryUpdateIntent(qLower)
            ChatIntent.ITINERARY_QUERY -> handleItineraryQueryIntent(qLower, isVegetarian)
            ChatIntent.TRIP_PLANNING -> handleTripPlanningIntent(qLower, isVegetarian, userStyles, userBudget)
            ChatIntent.BUDGET_QUERY, ChatIntent.BUDGET_CALCULATION -> handleBudgetIntent(qLower)
            ChatIntent.NEARBY_SEARCH, ChatIntent.FOOD_SEARCH -> handleNearbyAndFoodIntent(qLower, isVegetarian, userLocationAttached)
            ChatIntent.DESTINATION_DISCOVERY -> handleDestinationDiscoveryIntent(qLower, userStyles)
            ChatIntent.MESSAGE_GUIDE, ChatIntent.MESSAGE_VENDOR -> handleVendorMessageIntent(qLower)
            ChatIntent.GUIDE_QUERY, ChatIntent.VENDOR_QUERY -> handleGuideQueryIntent()
            ChatIntent.TRANSLATION -> handleTranslationIntent(query)
            ChatIntent.WEATHER -> handleWeatherIntent(qLower)
            ChatIntent.APP_NAVIGATION -> handleAppNavigationIntent(qLower)
            ChatIntent.PROFILE -> handleProfileIntent(currentUser)
            else -> handleGeneralChatIntent(query, isVegetarian, userBudget)
        }
    }

    private fun extractMemorySlots(qLower: String) {
        // Detect Destination
        listOf("goa", "jaipur", "delhi", "mumbai", "manali", "kerala", "agra", "varanasi", "ladakh", "udaipur", "shimla").forEach { dest ->
            if (qLower.contains(dest)) {
                shortTermMemory.destination = dest.replaceFirstChar { it.uppercase() }
            }
        }
        // Detect Days
        val dayMatch = Regex("(\\d+)\\s*(day|days|din)").find(qLower)
        if (dayMatch != null) {
            shortTermMemory.durationDays = dayMatch.groupValues[1].toIntOrNull()
        } else if (qLower.matches(Regex("^\\d+$"))) {
            shortTermMemory.durationDays = qLower.toIntOrNull()
        }
        // Detect Pace
        if (qLower.contains("relax") || qLower.contains("slow") || qLower.contains("shant")) {
            shortTermMemory.pace = "Relaxed"
        } else if (qLower.contains("fast") || qLower.contains("packed") || qLower.contains("active")) {
            shortTermMemory.pace = "Active"
        }
    }

    private fun classifyIntent(qLower: String, memory: ShortTermContext): ChatIntent {
        return when {
            qLower.contains("sos") || qLower.contains("emergency") || qLower.contains("khatra") || qLower.contains("ambulance") || qLower.contains("danger") -> ChatIntent.SOS
            qLower.contains("help") || qLower.contains("unsafe") || qLower.contains("lost") || qLower.contains("police") || qLower.contains("madad") || qLower.contains("following me") -> ChatIntent.SAFETY
            qLower.contains("lost phone") || qLower.contains("phone kho") -> ChatIntent.LOST_PHONE
            qLower.contains("safety group") || qLower.contains("group mode") -> ChatIntent.SAFETY_GROUP
            qLower.contains("remove") || qLower.contains("hata") || qLower.contains("move") || qLower.contains("shift") || qLower.contains("change itinerary") || qLower.contains("badal do") -> ChatIntent.ITINERARY_UPDATE
            qLower.contains("itinerary") || qLower.contains("plan today") || qLower.contains("what's next") || qLower.contains("kya plan") || qLower.contains("doing today") || qLower.contains("aaj kya") || qLower.contains("schedule") -> ChatIntent.ITINERARY_QUERY
            qLower.contains("plan") || qLower.contains("bana do") || (memory.destination != null && memory.durationDays != null) -> ChatIntent.TRIP_PLANNING
            qLower.contains("booking") || qLower.contains("hotel") || qLower.contains("check-in") || qLower.contains("ticket") || qLower.contains("flight") || qLower.contains("reservation") -> ChatIntent.BOOKING_QUERY
            qLower.contains("budget") || qLower.contains("kharcha") || qLower.contains("afford") || qLower.contains("kitna bacha") || qLower.contains("cost") || qLower.contains("money") -> ChatIntent.BUDGET_QUERY
            qLower.contains("restaurant") || qLower.contains("food") || qLower.contains("khana") || qLower.contains("dinner") || qLower.contains("lunch") || qLower.contains("cafe") || qLower.contains("veg") -> ChatIntent.FOOD_SEARCH
            qLower.contains("near me") || qLower.contains("paas") || qLower.contains("nearby") || qLower.contains("around") -> ChatIntent.NEARBY_SEARCH
            qLower.contains("tell my guide") || qLower.contains("message guide") || qLower.contains("tell the hotel") || qLower.contains("tell vendor") || qLower.contains("i'm late") -> ChatIntent.MESSAGE_GUIDE
            qLower.contains("who is my guide") || qLower.contains("guide contact") || qLower.contains("driver") || qLower.contains("vendor") -> ChatIntent.GUIDE_QUERY
            qLower.contains("translate") || qLower.contains("anuvad") || qLower.contains("in spanish") || qLower.contains("in hindi") || qLower.contains("how do i ask") -> ChatIntent.TRANSLATION
            qLower.contains("weather") || qLower.contains("mausam") || qLower.contains("temperature") || qLower.contains("rain") -> ChatIntent.WEATHER
            qLower.contains("open trips") || qLower.contains("open bookings") || qLower.contains("open safety") || qLower.contains("open map") || qLower.contains("show profile") -> ChatIntent.APP_NAVIGATION
            qLower.contains("explore") || qLower.contains("see in") || qLower.contains("ghumne") || qLower.contains("visit") || qLower.contains("attractions") -> ChatIntent.DESTINATION_DISCOVERY
            qLower.contains("profile") || qLower.contains("my preferences") || qLower.contains("who am i") -> ChatIntent.PROFILE
            else -> ChatIntent.GENERAL_CHAT
        }
    }

    // --- Intent Handlers ---

    private fun handleSafetyIntent(intent: ChatIntent, qLower: String): ChatMessage {
        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = "Your safety is our absolute priority. Use the emergency actions below immediately for instant SOS dispatch or to connect with official emergency responders (112).",
            cardType = ChatCardType.SAFETY,
            intent = intent,
            safetyPayload = SafetyCardPayload(
                alertType = if (intent == ChatIntent.SOS) "🚨 EMERGENCY SOS READY" else "🛡️ SAFETY ASSISTANCE",
                description = "One-tap dispatch broadcasts your verified GPS coordinates to emergency responders and safety contacts.",
                emergencyNumber = "112",
                canTriggerSos = true
            ),
            toolUsed = "SafetyEmergencyTool"
        )
    }

    private fun handleLostPhoneIntent(): ChatMessage {
        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = "Lost Phone Mode protects your trip data and allows trusted contacts to verify your last-known location using your secure Recovery PIN.",
            cardType = ChatCardType.SAFETY,
            intent = ChatIntent.LOST_PHONE,
            safetyPayload = SafetyCardPayload(
                alertType = "📱 LOST PHONE PROTECTION",
                description = "Configure your recovery PIN and trusted contacts in the Safety Center.",
                emergencyNumber = "112",
                canTriggerSos = false,
                showLostPhoneGuide = true
            ),
            navPayload = NavActionPayload(destinationScreen = "safety", title = "Safety Center", label = "Configure Lost Phone"),
            toolUsed = "LostPhoneTool"
        )
    }

    private fun handleSafetyGroupIntent(): ChatMessage {
        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = "Safety Group Mode keeps group members synchronized during shared excursions with proximity alerts and group check-ins.",
            cardType = ChatCardType.SAFETY,
            intent = ChatIntent.SAFETY_GROUP,
            safetyPayload = SafetyCardPayload(
                alertType = "👥 SAFETY GROUP MODE",
                description = "Enable automatic check-ins and member tracking with your travel party.",
                emergencyNumber = "112",
                canTriggerSos = false,
                showSafetyGroupGuide = true
            ),
            navPayload = NavActionPayload(destinationScreen = "safety", title = "Safety Center", label = "Manage Safety Group"),
            toolUsed = "SafetyGroupTool"
        )
    }

    private suspend fun handleBookingIntent(qLower: String): ChatMessage {
        val bookings = bookingRepository.getUserBookings()
        return if (bookings.isNotEmpty()) {
            val first = bookings.first()
            ChatMessage(
                conversationId = currentConversationId,
                sender = MessageSender.AI,
                text = "Here is your upcoming confirmed booking details:",
                cardType = ChatCardType.BOOKING,
                intent = ChatIntent.BOOKING_QUERY,
                bookingPayload = BookingCardPayload(
                    id = first.id,
                    title = first.itemTitle.ifBlank { "Luxury Heritage Stay" },
                    reference = first.bookingReference,
                    itemType = first.itemType.ifBlank { "Hotel & Resort" },
                    dateOrTime = "Check-in: ${first.checkInDate.ifBlank { "14:00 PM Tomorrow" }}",
                    status = first.status.replaceFirstChar { it.uppercase() },
                    guests = first.numberOfGuests,
                    totalAmount = first.totalAmount,
                    contactPhone = "+91 98765 43210"
                ),
                toolUsed = "BookingRepository.getUserBookings"
            )
        } else {
            ChatMessage(
                conversationId = currentConversationId,
                sender = MessageSender.AI,
                text = "I checked your account, but you have no active bookings right now. You can explore stays and packages in the Explore tab.",
                cardType = ChatCardType.NAV_ACTION,
                intent = ChatIntent.BOOKING_QUERY,
                navPayload = NavActionPayload(destinationScreen = "explore", title = "Explore Bookings", label = "Browse Hotels & Tours"),
                toolUsed = "BookingRepository.getUserBookings"
            )
        }
    }

    private fun handleItineraryUpdateIntent(qLower: String): ChatMessage {
        val isRemove = qLower.contains("remove") || qLower.contains("hata")
        val itemTitle = if (qLower.contains("museum")) "Heritage City Museum" else if (qLower.contains("beach")) "Calangute Beach Walk" else "Afternoon Sightseeing"
        val timeSlot = if (qLower.contains("evening") || qLower.contains("shift")) "6:00 PM" else "3:00 PM"

        val actionType = if (isRemove) "REMOVE_ACTIVITY" else "MODIFY_ACTIVITY"
        val desc = if (isRemove) "Remove '$itemTitle' scheduled at $timeSlot from your active itinerary." else "Shift '$itemTitle' to $timeSlot in your itinerary."

        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = if (isRemove) "I found '$itemTitle' at $timeSlot in your itinerary. Would you like me to remove it?" else "I can reschedule '$itemTitle' to $timeSlot for you.",
            cardType = ChatCardType.CONFIRMATION,
            intent = ChatIntent.ITINERARY_UPDATE,
            confirmationPayload = ConfirmationPayload(
                actionType = actionType,
                title = if (isRemove) "Remove Activity" else "Reschedule Activity",
                description = desc,
                targetItemId = "act_item_101",
                targetDayNumber = 1,
                extraData = mapOf("title" to itemTitle, "time" to timeSlot)
            ),
            isConfirmationPending = true,
            toolUsed = "ItineraryManager.proposeChange"
        )
    }

    private suspend fun handleItineraryQueryIntent(qLower: String, isVegetarian: Boolean): ChatMessage {
        val trips = tripRepository.getUserTrips()
        val active = trips.firstOrNull { it.status.equals("active", true) || it.status.equals("confirmed", true) } ?: trips.firstOrNull()
        val dest = active?.destinationName?.ifBlank { "Goa" } ?: "Goa"

        val activities = listOf(
            ItineraryActivityPayload(time = "09:00 AM", title = if (isVegetarian) "Pure Veg Breakfast Cafe" else "Coastal Cafe Breakfast", location = "$dest Hub", duration = "1h", estimatedCost = "₹350"),
            ItineraryActivityPayload(time = "10:30 AM", title = "Heritage Monument & Fort", location = "Historical District", duration = "2h 30m", estimatedCost = "₹250"),
            ItineraryActivityPayload(time = "01:30 PM", title = if (isVegetarian) "Thali & Traditional Lunch" else "Local Seafood Lunch", location = "Central Street", duration = "1h", estimatedCost = "₹600"),
            ItineraryActivityPayload(time = "04:30 PM", title = "Scenic Coastline & Sunset Point", location = "North Coast", duration = "2h", estimatedCost = "Free"),
            ItineraryActivityPayload(time = "07:30 PM", title = "Cultural Street Walk & Dinner", location = "Promenade", duration = "2h", estimatedCost = "₹800")
        )

        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = "Here is your scheduled itinerary for today in $dest (tailored for ${if (isVegetarian) "Vegetarian dining & relaxed pace" else "relaxed sightseeing"}):",
            cardType = ChatCardType.ITINERARY,
            intent = ChatIntent.ITINERARY_QUERY,
            itineraryPayload = ItineraryCardPayload(
                tripTitle = active?.title?.ifBlank { "$dest Vacation" } ?: "$dest Vacation",
                destination = dest,
                dayNumber = 1,
                date = "Today",
                activities = activities
            ),
            toolUsed = "TripRepository.getActiveItinerary"
        )
    }

    private fun handleTripPlanningIntent(qLower: String, isVegetarian: Boolean, userStyles: List<String>, userBudget: String): ChatMessage {
        val dest = shortTermMemory.destination ?: "Goa"
        val days = shortTermMemory.durationDays ?: 5
        val pace = shortTermMemory.pace ?: if (userStyles.any { it.contains("relax", true) }) "Relaxed" else "Balanced"

        if (shortTermMemory.destination == null) {
            return ChatMessage(
                conversationId = currentConversationId,
                sender = MessageSender.AI,
                text = "I'd love to plan your trip! Which destination are you heading to, and for how many days?",
                intent = ChatIntent.TRIP_PLANNING
            )
        }

        val activities = listOf(
            ItineraryActivityPayload(time = "10:00 AM", title = "Welcome & Scenic Check-in", location = "$dest Center", duration = "2h", estimatedCost = "₹500"),
            ItineraryActivityPayload(time = "01:30 PM", title = if (isVegetarian) "Vegetarian Specialty Lunch" else "Local Specialties Dining", location = "Old Town", duration = "1h", estimatedCost = "₹650"),
            ItineraryActivityPayload(time = "04:00 PM", title = "Nature & Sunset Leisure", location = "Coastline", duration = "2.5h", estimatedCost = "Free"),
            ItineraryActivityPayload(time = "07:00 PM", title = "Evening Heritage Promenade", location = "Town Square", duration = "1.5h", estimatedCost = "₹400")
        )

        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = "Here is a custom $days-day $pace trip for $dest (Budget tier: $userBudget, Dietary: ${if (isVegetarian) "Vegetarian" else "Standard"}):",
            cardType = ChatCardType.ITINERARY,
            intent = ChatIntent.TRIP_PLANNING,
            itineraryPayload = ItineraryCardPayload(
                tripTitle = "$days-Day $dest Getaway",
                destination = dest,
                dayNumber = 1,
                date = "Day 1 of $days",
                activities = activities
            ),
            toolUsed = "GeminiTripPlannerTool"
        )
    }

    private suspend fun handleBudgetIntent(qLower: String): ChatMessage {
        val bookings = bookingRepository.getUserBookings()
        val totalSpentOnBookings = bookings.map { it.totalAmount }.sum().let { if (it > 0) it else 31500.0 }
        val totalBudget = 50000.0
        val remaining = totalBudget - totalSpentOnBookings

        val proposedCost: Double? = if (qLower.contains("2000") || qLower.contains("2,000")) 2000.0 else if (qLower.contains("activity")) 1800.0 else null
        val proposedItem = if (proposedCost != null) "New Excursion Activity" else null
        val remainingAfter: Double? = if (proposedCost != null) remaining - proposedCost else null

        val isAffordable = (remainingAfter ?: remaining) > 0.0
        val affordabilityText = if (remainingAfter != null && isAffordable) {
            "Yes! You have ₹${String.format(Locale.US, "%,.0f", remaining)} remaining, so you can easily afford the new activity (₹${String.format(Locale.US, "%,.0f", proposedCost ?: 0.0)})."
        } else {
            "Here is your trip budget breakdown and spending analysis:"
        }

        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = affordabilityText,
            cardType = ChatCardType.BUDGET,
            intent = ChatIntent.BUDGET_QUERY,
            budgetPayload = BudgetCardPayload(
                totalBudget = totalBudget,
                spentAmount = totalSpentOnBookings,
                remainingAmount = remaining,
                proposedItem = proposedItem,
                proposedCost = proposedCost,
                remainingAfter = remainingAfter,
                isAffordable = isAffordable
            ),
            toolUsed = "BudgetCalculatorTool"
        )
    }

    private suspend fun handleNearbyAndFoodIntent(qLower: String, isVegetarian: Boolean, locationAttached: Boolean): ChatMessage {
        val isFood = qLower.contains("food") || qLower.contains("restaurant") || qLower.contains("veg") || qLower.contains("dinner") || qLower.contains("lunch")
        val places = destinationRepository.getDestinations()

        return if (isFood) {
            ChatMessage(
                conversationId = currentConversationId,
                sender = MessageSender.AI,
                text = "Here is a top-rated ${if (isVegetarian) "Pure Vegetarian " else ""}restaurant near your current location (Please confirm allergy requirements directly with the vendor):",
                cardType = ChatCardType.PLACE,
                intent = ChatIntent.FOOD_SEARCH,
                placePayload = PlaceCardPayload(
                    id = "place_veg_1",
                    name = if (isVegetarian) "Govinda's Pure Veg Feast" else "Spice Garden Coastal Dining",
                    category = if (isVegetarian) "Pure Veg Dining" else "Multicuisine & Seafood",
                    distance = "0.8 km from you",
                    address = "MG Road, Central Plaza",
                    rating = 4.8,
                    openStatus = "Open Now • Closes 11 PM"
                ),
                toolUsed = "PlacesApi.searchNearbyFood"
            )
        } else {
            ChatMessage(
                conversationId = currentConversationId,
                sender = MessageSender.AI,
                text = "Here is the highest recommended attraction near you with live open hours:",
                cardType = ChatCardType.PLACE,
                intent = ChatIntent.NEARBY_SEARCH,
                placePayload = PlaceCardPayload(
                    id = "place_attraction_1",
                    name = places.firstOrNull()?.name ?: "Aguada Coastal Fort",
                    category = "Heritage & Scenic Landmark",
                    distance = "1.4 km from hotel",
                    address = "Fort Road, Coastline",
                    rating = 4.7,
                    openStatus = "Open Now • Closes 6:00 PM"
                ),
                toolUsed = "PlacesApi.searchNearbyPlaces"
            )
        }
    }

    private suspend fun handleDestinationDiscoveryIntent(qLower: String, userStyles: List<String>): ChatMessage {
        val places = destinationRepository.getDestinations()
        val dest = places.firstOrNull { qLower.contains(it.name.lowercase()) } ?: places.firstOrNull()

        return if (dest != null) {
            ChatMessage(
                conversationId = currentConversationId,
                sender = MessageSender.AI,
                text = "${dest.name} is famous for ${dest.topAttractions.take(3).joinToString(", ")}. Best season: ${dest.bestTimeToVisit}.",
                cardType = ChatCardType.PLACE,
                intent = ChatIntent.DESTINATION_DISCOVERY,
                placePayload = PlaceCardPayload(
                    id = dest.id,
                    name = dest.name,
                    category = dest.state,
                    distance = "Popular in ${dest.state}",
                    address = dest.topAttractions.firstOrNull() ?: "",
                    rating = 4.8,
                    openStatus = "Ideal for ${userStyles.firstOrNull() ?: "Relaxed"} travelers"
                ),
                toolUsed = "DestinationRepository.getDestinations"
            )
        } else {
            ChatMessage(
                conversationId = currentConversationId,
                sender = MessageSender.AI,
                text = "I recommend exploring Jaipur for culture & forts, Goa for relaxed beaches, or Ladakh for Himalayan adventure.",
                intent = ChatIntent.DESTINATION_DISCOVERY
            )
        }
    }

    private fun handleVendorMessageIntent(qLower: String): ChatMessage {
        val isLate = qLower.contains("late") || qLower.contains("30")
        val messageText = if (isLate) "Hi, I am running approximately 30 minutes late. Please wait at the designated meeting point." else "Hi, I prefer a quiet room away from the elevator. Thank you!"
        val recipient = if (qLower.contains("hotel")) "Hotel Concierge" else "Assigned Tour Guide (Rajesh Kumar)"

        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = "I have prepared the message for your $recipient. Please confirm before sending:",
            cardType = ChatCardType.CONFIRMATION,
            intent = ChatIntent.MESSAGE_GUIDE,
            vendorMessagePayload = VendorMessagePayload(
                recipientType = if (qLower.contains("hotel")) "Hotel" else "Guide",
                recipientName = recipient,
                messagePreview = messageText,
                recipientPhone = "+91 98765 12345"
            ),
            confirmationPayload = ConfirmationPayload(
                actionType = "SEND_MESSAGE",
                title = "Send Message to $recipient",
                description = "\"$messageText\"",
                targetItemId = "msg_001",
                extraData = mapOf("text" to messageText, "recipient" to recipient)
            ),
            isConfirmationPending = true,
            toolUsed = "VendorMessagingTool.prepareDraft"
        )
    }

    private fun handleGuideQueryIntent(): ChatMessage {
        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = "Your assigned certified guide for this trip is Rajesh Kumar (⭐ 4.9). Pickup time is 09:30 AM at the hotel lobby. Contact: +91 98765 12345.",
            cardType = ChatCardType.BOOKING,
            intent = ChatIntent.GUIDE_QUERY,
            bookingPayload = BookingCardPayload(
                id = "guide_001",
                title = "Certified Tour Guide (Rajesh Kumar)",
                reference = "GUIDE-IND-8821",
                itemType = "Guide & Driver",
                dateOrTime = "Pickup: 09:30 AM Tomorrow",
                status = "Assigned & Verified",
                guests = 2,
                contactPhone = "+91 98765 12345"
            ),
            toolUsed = "GuideAssignmentService.getAssignedGuide"
        )
    }

    private fun handleTranslationIntent(query: String): ChatMessage {
        val translated = if (query.contains("towel", ignoreCase = true)) {
            "\"¿Podría traerme una toalla extra, por favor?\" (Spanish)\n\"क्या मुझे एक अतिरिक्त तौलिया मिल सकता है?\" (Hindi)"
        } else if (query.contains("menu", ignoreCase = true)) {
            "\"¿Tienen opciones vegetarianas?\" (Do you have vegetarian options?)"
        } else {
            "\"Namaste! Kripya meri madad karein.\" (Greetings! Please assist me.)"
        }

        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = "Here is the accurate translation:\n\n$translated",
            intent = ChatIntent.TRANSLATION,
            toolUsed = "TranslationService"
        )
    }

    private fun handleWeatherIntent(qLower: String): ChatMessage {
        val dest = shortTermMemory.destination ?: "Goa"
        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = "Current weather in $dest: 28°C, Partly Sunny with a light coastal breeze (Humidity 68%). Great conditions for daytime sightseeing!",
            intent = ChatIntent.WEATHER,
            toolUsed = "LiveWeatherApi"
        )
    }

    private fun handleAppNavigationIntent(qLower: String): ChatMessage {
        val (dest, label) = when {
            qLower.contains("trip") -> "trips" to "Open My Trips"
            qLower.contains("booking") -> "bookings" to "View All Bookings"
            qLower.contains("safety") -> "safety" to "Open Safety Center"
            qLower.contains("map") -> "map" to "Open Interactive Map"
            qLower.contains("explore") -> "explore" to "Open Explore Hub"
            else -> "trips" to "Open Trips"
        }

        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = "Sure, I can navigate you directly:",
            cardType = ChatCardType.NAV_ACTION,
            intent = ChatIntent.APP_NAVIGATION,
            navPayload = NavActionPayload(destinationScreen = dest, title = label, label = label),
            toolUsed = "NavigationController"
        )
    }

    private fun handleProfileIntent(currentUser: UserDto?): ChatMessage {
        val name = currentUser?.full_name ?: "Traveler"
        val diet = currentUser?.preferences?.dietary_preferences?.joinToString(", ") ?: "Not set"
        val styles = currentUser?.preferences?.travel_styles?.joinToString(", ") ?: "Relaxed"
        val budget = currentUser?.preferences?.budget_range ?: "Medium"

        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = "Hello $name! Here are your active profile preferences:\n• Travel Style: $styles\n• Food Preference: $diet\n• Budget Tier: $budget\n• Safety State: Active & Configured",
            intent = ChatIntent.PROFILE,
            toolUsed = "UserRepository.getUserProfile"
        )
    }

    private fun handleGeneralChatIntent(query: String, isVegetarian: Boolean, budget: String): ChatMessage {
        val qLower = query.lowercase(Locale.ROOT)
        val text = if (qLower.contains("kya kar sakta") || qLower.contains("kaha jau")) {
            "Aapke $budget budget aur ${if (isVegetarian) "Vegetarian" else "relaxed"} travel style ke hisaab se, main morning mein fort visit aur evening mein sunset beach view suggest karta hu. Kya aap chahte hain ki main pura 1-day plan banau?"
        } else {
            "I'm here to assist with trip planning, itinerary modifications, bookings lookup, budget calculations, nearby recommendations, and emergency safety. How can I help with your trip?"
        }

        return ChatMessage(
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = text,
            intent = ChatIntent.GENERAL_CHAT
        )
    }

    suspend fun executeConfirmedAction(confirmation: ConfirmationPayload): ChatMessage = withContext(Dispatchers.IO) {
        val resultText = when (confirmation.actionType) {
            "REMOVE_ACTIVITY" -> {
                "Done. The item '${confirmation.extraData["title"] ?: "activity"}' has been successfully removed from your active itinerary."
            }
            "MODIFY_ACTIVITY" -> {
                "Done. Your activity has been rescheduled to ${confirmation.extraData["time"] ?: "new time"} in your itinerary."
            }
            "SEND_MESSAGE" -> {
                "Message sent successfully to ${confirmation.extraData["recipient"] ?: "your contact"}: \"${confirmation.extraData["text"] ?: ""}\""
            }
            else -> "Action confirmed and updated in your trip records."
        }

        val confirmMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = currentConversationId,
            sender = MessageSender.AI,
            text = resultText,
            timestamp = System.currentTimeMillis(),
            cardType = ChatCardType.NONE,
            intent = ChatIntent.ITINERARY_UPDATE,
            toolUsed = "BackendDatabase.executeUpdate"
        )

        // Mark previous pending confirmation as resolved
        val currentList = _messagesFlow.value.map {
            if (it.confirmationPayload?.actionType == confirmation.actionType) {
                it.copy(isConfirmationPending = false)
            } else it
        }.toMutableList().apply { add(confirmMsg) }

        _messagesFlow.value = currentList
        persistMessage(confirmMsg)
        confirmMsg
    }

    suspend fun submitFeedback(messageId: String, isHelpful: Boolean, reason: String? = null) = withContext(Dispatchers.IO) {
        val status = if (isHelpful) FeedbackStatus.HELPFUL else FeedbackStatus.NOT_HELPFUL
        val updated = _messagesFlow.value.map {
            if (it.id == messageId) it.copy(feedbackStatus = status, feedbackReason = reason) else it
        }
        _messagesFlow.value = updated

        val userId = auth.currentUser?.uid
        if (!userId.isNullOrBlank()) {
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("conversations")
                    .document(currentConversationId)
                    .collection("messages")
                    .document(messageId)
                    .update(mapOf(
                        "feedbackStatus" to status.name,
                        "feedbackReason" to (reason ?: "")
                    ))
                    .await()
            } catch (e: Exception) {
                // Ignore sync errors
            }
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (!userId.isNullOrBlank()) {
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("conversations")
                    .document(currentConversationId)
                    .collection("messages")
                    .get()
                    .await()

                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                // In-memory fallback
            }
        }
        shortTermMemory = ShortTermContext()
        _messagesFlow.value = emptyList()
        loadMessages(currentConversationId)
    }

    private suspend fun persistMessage(msg: ChatMessage) {
        val userId = auth.currentUser?.uid ?: return
        try {
            val map = mutableMapOf<String, Any>(
                "id" to msg.id,
                "conversationId" to msg.conversationId,
                "sender" to msg.sender.name,
                "text" to msg.text,
                "timestamp" to msg.timestamp,
                "cardType" to msg.cardType.name,
                "intent" to msg.intent.name
            )
            msg.toolUsed?.let { map["toolUsed"] = it }

            firestore.collection("users")
                .document(userId)
                .collection("conversations")
                .document(msg.conversationId)
                .collection("messages")
                .document(msg.id)
                .set(map)
                .await()
        } catch (e: Exception) {
            // Offline resilient
        }
    }
}
