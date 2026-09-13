package com.touristapp.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.touristapp.data.models.ChatMessage
import com.touristapp.data.models.ChatQuickAction
import com.touristapp.data.models.ConfirmationPayload
import com.touristapp.data.repository.AiAssistantRepository
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AiAssistantViewModel(
    private val repository: AiAssistantRepository = ServiceLocator.aiAssistantRepository
) : ViewModel() {

    val messages: StateFlow<List<ChatMessage>> = repository.messagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    val quickActions = listOf(
        ChatQuickAction(
            id = "plan_trip",
            iconEmoji = "🗺️",
            title = "Plan My Trip",
            subtitle = "Customized day-by-day plan",
            prompt = "Plan a 3-day relaxed trip for me."
        ),
        ChatQuickAction(
            id = "itinerary",
            iconEmoji = "📅",
            title = "My Itinerary",
            subtitle = "What am I doing today?",
            prompt = "What is on my itinerary for today?"
        ),
        ChatQuickAction(
            id = "booking",
            iconEmoji = "🏨",
            title = "My Bookings",
            subtitle = "Check-in, stays & tours",
            prompt = "Show my active hotel and tour bookings."
        ),
        ChatQuickAction(
            id = "nearby",
            iconEmoji = "📍",
            title = "Nearby Places",
            subtitle = "Attractions & sights near me",
            prompt = "What interesting places and attractions are near me?"
        ),
        ChatQuickAction(
            id = "budget",
            iconEmoji = "💰",
            title = "Budget",
            subtitle = "Spent vs remaining balance",
            prompt = "Can I afford another activity with my remaining budget?"
        ),
        ChatQuickAction(
            id = "food",
            iconEmoji = "🍴",
            title = "Food & Stay",
            subtitle = "Vegetarian & local dining",
            prompt = "Recommend top-rated vegetarian food near my stay."
        ),
        ChatQuickAction(
            id = "guide",
            iconEmoji = "🧑‍✈️",
            title = "Guide / Vendor",
            subtitle = "Tour guide & driver info",
            prompt = "Who is my assigned guide and what is our next pickup time?"
        ),
        ChatQuickAction(
            id = "safety",
            iconEmoji = "🛡️",
            title = "Safety",
            subtitle = "Emergency help & guidance",
            prompt = "I need safety assistance and emergency help."
        ),
        ChatQuickAction(
            id = "translate",
            iconEmoji = "🌐",
            title = "Translate",
            subtitle = "Essential travel phrases",
            prompt = "Translate essential travel phrases for food and transport."
        )
    )

    init {
        viewModelScope.launch {
            repository.getOrCreateConversation()
        }
    }

    fun onInputChanged(text: String) {
        _inputText.value = text
    }

    fun sendMessage(customPrompt: String? = null, isLocationAttached: Boolean = false) {
        val textToSend = (customPrompt ?: _inputText.value).trim()
        if (textToSend.isBlank()) return

        if (customPrompt == null) {
            _inputText.value = ""
        }

        viewModelScope.launch {
            _isTyping.value = true
            try {
                repository.sendMessage(textToSend, isLocationAttached)
            } finally {
                _isTyping.value = false
            }
        }
    }

    fun confirmAction(action: ConfirmationPayload) {
        viewModelScope.launch {
            _isTyping.value = true
            try {
                repository.executeConfirmedAction(action)
            } finally {
                _isTyping.value = false
            }
        }
    }

    fun submitFeedback(messageId: String, isHelpful: Boolean, reason: String? = null) {
        viewModelScope.launch {
            repository.submitFeedback(messageId, isHelpful, reason)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}

class AiAssistantViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AiAssistantViewModel() as T
    }
}
