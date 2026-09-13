package com.travellikepro.travelvendor.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.travelvendor.data.model.Notification
import com.travellikepro.travelvendor.data.repository.NotificationsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    data class Success(val notifications: List<Notification>, val unreadCount: Int) : NotificationsUiState()
    object Empty : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotificationsRepository(application)

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            try {
                val response = repository.getNotifications()
                if (response.success && response.data != null) {
                    val list = response.data.notifications
                    if (list.isEmpty()) {
                        _uiState.value = NotificationsUiState.Empty
                    } else {
                        _uiState.value = NotificationsUiState.Success(list, response.data.unreadCount)
                    }
                } else {
                    _uiState.value = NotificationsUiState.Error(response.message ?: "Failed to load notifications")
                }
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.localizedMessage ?: "Network error loading notifications")
            }
        }
    }

    fun markAsRead(id: Int) {
        viewModelScope.launch {
            try {
                repository.markAsRead(id)
                loadNotifications()
            } catch (_: Exception) {}
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                repository.markAllAsRead()
                loadNotifications()
            } catch (_: Exception) {}
        }
    }
}
