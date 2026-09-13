package com.touristapp.presentation.guide

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.location.LocationService
import com.touristapp.data.models.*
import com.touristapp.data.repository.GuideRepository
import com.touristapp.data.repository.SessionManager
import com.touristapp.data.speech.SpeechAndTtsManager
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CameraTranslationUiState(
    val sourceLang: String = "auto",
    val targetLang: String = "en",
    val isAnalyzing: Boolean = false,
    val originalText: String? = null,
    val detectedSourceLang: String? = null,
    val translatedText: String? = null,
    val isSaved: Boolean = false,
    val flashEnabled: Boolean = false,
    val errorMessage: String? = null
)

data class PlaceDetectionUiState(
    val isAnalyzing: Boolean = false,
    val detectedLandmark: LandmarkInfo? = null,
    val isSaved: Boolean = false,
    val flashEnabled: Boolean = false,
    val errorMessage: String? = null
)

data class VoiceTranslationUiState(
    val travelerLang: String = "en",
    val localLang: String = "hi",
    val activeSpeaker: VoiceSender? = null,
    val currentSpokenText: String = "",
    val conversationMessages: List<VoiceChatMessage> = emptyList(),
    val errorMessage: String? = null
)

data class TravelGuideUiState(
    val searchQuery: String = "Agra",
    val isLoading: Boolean = false,
    val destinationInfo: TravelGuideDestination? = null,
    val errorMessage: String? = null
)

class GuideViewModel(
    private val guideRepository: GuideRepository = ServiceLocator.guideRepository,
    private val speechAndTtsManager: SpeechAndTtsManager = ServiceLocator.speechAndTtsManager,
    private val locationService: LocationService = ServiceLocator.locationService,
    private val sessionManager: SessionManager = ServiceLocator.sessionManager
) : ViewModel() {

    private val _cameraTransState = MutableStateFlow(CameraTranslationUiState())
    val cameraTransState: StateFlow<CameraTranslationUiState> = _cameraTransState.asStateFlow()

    private val _placeDetectState = MutableStateFlow(PlaceDetectionUiState())
    val placeDetectState: StateFlow<PlaceDetectionUiState> = _placeDetectState.asStateFlow()

    private val _voiceTransState = MutableStateFlow(VoiceTranslationUiState())
    val voiceTransState: StateFlow<VoiceTranslationUiState> = _voiceTransState.asStateFlow()

    private val _travelGuideState = MutableStateFlow(TravelGuideUiState())
    val travelGuideState: StateFlow<TravelGuideUiState> = _travelGuideState.asStateFlow()

    val translationsList: StateFlow<List<TranslationRecord>> = guideRepository.translationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPlacesList: StateFlow<List<SavedPlaceItem>> = guideRepository.savedPlacesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isListening: StateFlow<Boolean> = speechAndTtsManager.isListening
    val isSpeaking: StateFlow<Boolean> = speechAndTtsManager.isSpeaking

    init {
        viewModelScope.launch {
            // Load user data & set default target language from profile
            sessionManager.currentUser.collect { user ->
                user?.let {
                    guideRepository.loadUserData(it.id)
                }
            }
        }
        // Load initial destination guide
        searchDestination("Agra")
    }

    // ==========================================
    // 1. Camera Translation
    // ==========================================
    fun setCameraSourceLang(lang: String) {
        _cameraTransState.update { it.copy(sourceLang = lang) }
    }

    fun setCameraTargetLang(lang: String) {
        _cameraTransState.update { it.copy(targetLang = lang) }
    }

    fun toggleCameraFlash() {
        _cameraTransState.update { it.copy(flashEnabled = !it.flashEnabled) }
    }

    fun swapCameraLanguages() {
        _cameraTransState.update { state ->
            val src = if (state.sourceLang == "auto") "en" else state.sourceLang
            state.copy(sourceLang = state.targetLang, targetLang = src)
        }
    }

    fun processCapturedText(rawText: String) {
        if (rawText.isBlank()) {
            _cameraTransState.update { it.copy(errorMessage = "No text found in camera frame. Please try again.") }
            return
        }

        viewModelScope.launch {
            _cameraTransState.update { it.copy(isAnalyzing = true, errorMessage = null, isSaved = false) }
            try {
                val (detectedLang, translated) = guideRepository.translateText(
                    text = rawText,
                    sourceLang = _cameraTransState.value.sourceLang,
                    targetLang = _cameraTransState.value.targetLang
                )
                _cameraTransState.update {
                    it.copy(
                        isAnalyzing = false,
                        originalText = rawText,
                        detectedSourceLang = detectedLang,
                        translatedText = translated,
                        isSaved = false
                    )
                }
            } catch (e: Exception) {
                _cameraTransState.update {
                    it.copy(isAnalyzing = false, errorMessage = "Translation error: ${e.message}")
                }
            }
        }
    }

    fun saveCurrentTranslation() {
        val original = _cameraTransState.value.originalText ?: return
        val translated = _cameraTransState.value.translatedText ?: return
        val src = _cameraTransState.value.detectedSourceLang ?: _cameraTransState.value.sourceLang
        val tgt = _cameraTransState.value.targetLang

        viewModelScope.launch {
            val record = TranslationRecord(
                sourceLanguage = src,
                targetLanguage = tgt,
                originalText = original,
                translatedText = translated
            )
            val success = guideRepository.saveTranslation(record)
            if (success) {
                _cameraTransState.update { it.copy(isSaved = true) }
            }
        }
    }

    fun playTranslationAudio(text: String, lang: String) {
        speechAndTtsManager.speak(text, lang)
    }

    fun clearCameraTranslation() {
        _cameraTransState.update {
            it.copy(
                originalText = null,
                detectedSourceLang = null,
                translatedText = null,
                isSaved = false,
                errorMessage = null
            )
        }
    }

    // ==========================================
    // 2. AI Place Detection
    // ==========================================
    fun togglePlaceFlash() {
        _placeDetectState.update { it.copy(flashEnabled = !it.flashEnabled) }
    }

    fun scanAndDetectPlace(capturedHint: String? = null) {
        viewModelScope.launch {
            _placeDetectState.update { it.copy(isAnalyzing = true, errorMessage = null, isSaved = false) }
            try {
                val location = try { locationService.getCurrentLocation() } catch (e: Exception) { null }
                val landmark = guideRepository.identifyLandmark(
                    capturedText = capturedHint,
                    userLat = location?.latitude,
                    userLon = location?.longitude
                )
                _placeDetectState.update {
                    it.copy(
                        isAnalyzing = false,
                        detectedLandmark = landmark,
                        isSaved = false
                    )
                }
            } catch (e: Exception) {
                _placeDetectState.update {
                    it.copy(isAnalyzing = false, errorMessage = "Could not identify landmark: ${e.message}")
                }
            }
        }
    }

    fun saveDetectedLandmark() {
        val landmark = _placeDetectState.value.detectedLandmark ?: return
        viewModelScope.launch {
            val place = SavedPlaceItem(
                placeName = landmark.name,
                location = "${landmark.city}, ${landmark.country}",
                description = landmark.history,
                imageUrl = landmark.imageUrl,
                latitude = landmark.latitude,
                longitude = landmark.longitude,
                category = landmark.whyFamous
            )
            val success = guideRepository.savePlace(place)
            if (success) {
                _placeDetectState.update { it.copy(isSaved = true) }
            }
        }
    }

    fun clearDetectedPlace() {
        _placeDetectState.update {
            it.copy(
                detectedLandmark = null,
                isSaved = false,
                errorMessage = null
            )
        }
    }

    // ==========================================
    // 3. Live Voice Translation & Conversation
    // ==========================================
    fun setTravelerLang(lang: String) {
        _voiceTransState.update { it.copy(travelerLang = lang) }
    }

    fun setLocalLang(lang: String) {
        _voiceTransState.update { it.copy(localLang = lang) }
    }

    fun swapConversationLanguages() {
        _voiceTransState.update {
            it.copy(travelerLang = it.localLang, localLang = it.travelerLang)
        }
    }

    fun startVoiceListening(sender: VoiceSender) {
        val srcLang = if (sender == VoiceSender.TRAVELER) _voiceTransState.value.travelerLang else _voiceTransState.value.localLang
        val tgtLang = if (sender == VoiceSender.TRAVELER) _voiceTransState.value.localLang else _voiceTransState.value.travelerLang

        _voiceTransState.update { it.copy(activeSpeaker = sender, currentSpokenText = "", errorMessage = null) }

        speechAndTtsManager.startListening(
            languageCode = srcLang,
            onPartialResult = { partial ->
                _voiceTransState.update { it.copy(currentSpokenText = partial) }
            },
            onResult = { spokenText ->
                _voiceTransState.update { it.copy(currentSpokenText = spokenText, activeSpeaker = null) }
                handleVoiceTranslationResult(spokenText, sender, srcLang, tgtLang)
            },
            onError = { err ->
                _voiceTransState.update { it.copy(errorMessage = err, activeSpeaker = null) }
            }
        )
    }

    fun stopVoiceListening() {
        speechAndTtsManager.stopListening()
        _voiceTransState.update { it.copy(activeSpeaker = null) }
    }

    private fun handleVoiceTranslationResult(
        spokenText: String,
        sender: VoiceSender,
        srcLang: String,
        tgtLang: String
    ) {
        viewModelScope.launch {
            val (_, translated) = guideRepository.translateText(spokenText, srcLang, tgtLang)
            val message = VoiceChatMessage(
                sender = sender,
                originalText = spokenText,
                translatedText = translated,
                sourceLang = srcLang,
                targetLang = tgtLang
            )
            _voiceTransState.update {
                it.copy(conversationMessages = it.conversationMessages + message)
            }
            // Auto speak translation aloud in target language
            speechAndTtsManager.speak(translated, tgtLang)
        }
    }

    fun clearVoiceConversation() {
        _voiceTransState.update { it.copy(conversationMessages = emptyList(), currentSpokenText = "", errorMessage = null) }
    }

    // ==========================================
    // 4. AI Travel Guide
    // ==========================================
    fun searchDestination(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _travelGuideState.update { it.copy(searchQuery = query, isLoading = true, errorMessage = null) }
            try {
                val info = guideRepository.getTravelGuideDestination(query)
                _travelGuideState.update { it.copy(isLoading = false, destinationInfo = info) }
            } catch (e: Exception) {
                _travelGuideState.update { it.copy(isLoading = false, errorMessage = "Could not fetch guide info: ${e.message}") }
            }
        }
    }

    // ==========================================
    // 5. Saved Items Management
    // ==========================================
    fun deleteTranslation(id: String) {
        viewModelScope.launch {
            guideRepository.deleteTranslation(id)
        }
    }

    fun deleteSavedPlace(id: String) {
        viewModelScope.launch {
            guideRepository.deleteSavedPlace(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechAndTtsManager.release()
    }
}
