package com.travellikepro.travelvendor.ui.listings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.travelvendor.data.model.CreateListingRequest
import com.travellikepro.travelvendor.data.model.Listing
import com.travellikepro.travelvendor.data.repository.ListingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ListingsUiState {
    object Loading : ListingsUiState()
    data class Success(val listings: List<Listing>, val totalCount: Int) : ListingsUiState()
    object Empty : ListingsUiState()
    data class Error(val message: String) : ListingsUiState()
}

class ListingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ListingsRepository(application)

    private val _uiState = MutableStateFlow<ListingsUiState>(ListingsUiState.Loading)
    val uiState: StateFlow<ListingsUiState> = _uiState

    private val _detailState = MutableStateFlow<Listing?>(null)
    val detailState: StateFlow<Listing?> = _detailState

    private val _isOperating = MutableStateFlow(false)
    val isOperating: StateFlow<Boolean> = _isOperating

    var currentStatusFilter: String? = null
    var currentSearchQuery: String = ""

    init {
        loadListings()
    }

    fun loadListings(status: String? = currentStatusFilter, search: String = currentSearchQuery) {
        currentStatusFilter = status
        currentSearchQuery = search

        viewModelScope.launch {
            _uiState.value = ListingsUiState.Loading
            try {
                val response = repository.getListings(status = status, search = search.ifBlank { null })
                if (response.success && response.data != null) {
                    val list = response.data.listings
                    if (list.isEmpty()) {
                        _uiState.value = ListingsUiState.Empty
                    } else {
                        _uiState.value = ListingsUiState.Success(list, response.data.total)
                    }
                } else {
                    _uiState.value = ListingsUiState.Error(response.message ?: "Failed to load listings")
                }
            } catch (e: Exception) {
                _uiState.value = ListingsUiState.Error(e.localizedMessage ?: "Network error loading listings")
            }
        }
    }

    fun loadListingDetail(id: Int) {
        viewModelScope.launch {
            try {
                val response = repository.getListingById(id)
                if (response.success) {
                    _detailState.value = response.data
                }
            } catch (_: Exception) {}
        }
    }

    fun createListing(request: CreateListingRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isOperating.value = true
            try {
                val response = repository.createListing(request)
                if (response.success) {
                    loadListings()
                    onSuccess()
                }
            } catch (_: Exception) {
            } finally {
                _isOperating.value = false
            }
        }
    }

    fun updateListing(id: Int, request: CreateListingRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isOperating.value = true
            try {
                val response = repository.updateListing(id, request)
                if (response.success) {
                    loadListings()
                    onSuccess()
                }
            } catch (_: Exception) {
            } finally {
                _isOperating.value = false
            }
        }
    }

    fun deleteListing(id: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isOperating.value = true
            try {
                val response = repository.deleteListing(id)
                if (response.success) {
                    loadListings()
                    onSuccess()
                }
            } catch (_: Exception) {
            } finally {
                _isOperating.value = false
            }
        }
    }
}
