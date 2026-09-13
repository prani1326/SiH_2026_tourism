package com.travellikepro.opsleader.ui.vendors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.opsleader.data.api.VendorDto
import com.travellikepro.opsleader.data.repository.OperationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class VendorsUiState {
    object Loading : VendorsUiState()
    data class Success(val vendors: List<VendorDto>) : VendorsUiState()
    object Empty : VendorsUiState()
    data class Error(val message: String) : VendorsUiState()
}

@HiltViewModel
class VendorsViewModel @Inject constructor(
    private val operationsRepository: OperationsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VendorsUiState>(VendorsUiState.Loading)
    val uiState: StateFlow<VendorsUiState> = _uiState.asStateFlow()

    private var allVendors: List<VendorDto> = emptyList()
    private var selectedCategory: String = "ALL"
    private var searchQuery: String = ""

    init {
        loadVendors()
    }

    fun loadVendors() {
        _uiState.value = VendorsUiState.Loading
        viewModelScope.launch {
            val result = operationsRepository.getPartners()
            result.fold(
                onSuccess = { vendors ->
                    allVendors = vendors
                    applyFilters()
                },
                onFailure = { error ->
                    _uiState.value = VendorsUiState.Error(
                        error.localizedMessage ?: "Failed to fetch partners/vendors"
                    )
                }
            )
        }
    }

    fun setCategoryFilter(category: String) {
        selectedCategory = category
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = allVendors.filter { vendor ->
            val vType = vendor.service_type ?: vendor.serviceType ?: ""
            val matchesCategory = when (selectedCategory.uppercase()) {
                "ALL" -> true
                else -> vType.contains(selectedCategory, ignoreCase = true)
            }

            val matchesSearch = searchQuery.isBlank() ||
                vendor.name.contains(searchQuery, ignoreCase = true) ||
                (vendor.contact_phone?.contains(searchQuery, ignoreCase = true) == true) ||
                (vendor.contactPhone?.contains(searchQuery, ignoreCase = true) == true) ||
                vendor.region_coverage.any { it.contains(searchQuery, ignoreCase = true) } ||
                vendor.regionCoverage.any { it.contains(searchQuery, ignoreCase = true) }

            matchesCategory && matchesSearch
        }

        if (filtered.isEmpty()) {
            _uiState.value = VendorsUiState.Empty
        } else {
            _uiState.value = VendorsUiState.Success(filtered)
        }
    }
}
