package com.touristapp.di

import android.content.Context
import com.touristapp.data.repository.*

object ServiceLocator {
    lateinit var sessionRepository: SessionRepository
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var destinationRepository: DestinationRepository
        private set

    lateinit var tripRepository: TripRepository
        private set

    lateinit var bookingRepository: BookingRepository
        private set

    lateinit var safetyRepository: SafetyRepository
        private set

    lateinit var communityRepository: CommunityRepository
        private set

    lateinit var supportRepository: SupportRepository
        private set

    lateinit var toolsRepository: ToolsRepository
        private set

    lateinit var mapRepository: MapRepository
        private set

    lateinit var locationService: com.touristapp.data.location.LocationService
        private set

    lateinit var backendApiClient: com.touristapp.data.remote.BackendApiClient
        private set

    lateinit var aiAssistantRepository: AiAssistantRepository
        private set

    lateinit var guideRepository: GuideRepository
        private set

    lateinit var speechAndTtsManager: com.touristapp.data.speech.SpeechAndTtsManager
        private set

    fun initialize(context: Context) {
        if (!this::sessionRepository.isInitialized) {
            backendApiClient = com.touristapp.data.remote.BackendApiClient()
            sessionRepository = SessionRepository(context.applicationContext)
            sessionManager = SessionManager(sessionRepository)
            authRepository = AuthRepository(sessionRepository, sessionManager)
            userRepository = UserRepository(sessionManager)
            destinationRepository = DestinationRepository(backendApiClient = backendApiClient)
            tripRepository = TripRepository(backendApiClient = backendApiClient)
            bookingRepository = BookingRepository(backendApiClient = backendApiClient)
            communityRepository = CommunityRepository(backendApiClient, tripRepository)
            supportRepository = SupportRepository(backendApiClient)
            toolsRepository = ToolsRepository(backendApiClient)
            mapRepository = MapRepository()
            locationService = com.touristapp.data.location.LocationService(context.applicationContext)
            val safetyLocalStore = com.touristapp.data.local.SafetyLocalStore(context.applicationContext)
            val heartbeatManager = com.touristapp.data.safety.DeviceHeartbeatManager(context.applicationContext, locationService)
            safetyRepository = SafetyRepository(
                backendApiClient = backendApiClient,
                localStore = safetyLocalStore,
                locationService = locationService,
                heartbeatManager = heartbeatManager
            )
            aiAssistantRepository = AiAssistantRepository(
                backendApiClient = backendApiClient,
                tripRepository = tripRepository,
                bookingRepository = bookingRepository,
                destinationRepository = destinationRepository,
                safetyRepository = safetyRepository,
                userRepository = userRepository,
                locationService = locationService
            )
            guideRepository = GuideRepository(
                backendApiClient = backendApiClient
            )
            speechAndTtsManager = com.touristapp.data.speech.SpeechAndTtsManager(context.applicationContext)
        }
    }
}
