package com.touristapp.presentation.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.models.DestinationDto
import com.touristapp.data.models.LocalExperienceDto
import com.touristapp.data.remote.ApiResult
import com.touristapp.data.remote.BackendApiClient
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val destinationRepository: com.touristapp.data.repository.DestinationRepository = ServiceLocator.destinationRepository,
    private val apiClient: BackendApiClient = ServiceLocator.backendApiClient
) : ViewModel() {

    private val _destinations = MutableStateFlow<ApiResult<List<DestinationDto>>>(ApiResult.Loading)
    val destinations: StateFlow<ApiResult<List<DestinationDto>>> = _destinations

    private val _localExperiences = MutableStateFlow<List<LocalExperienceDto>>(emptyList())
    val localExperiences: StateFlow<List<LocalExperienceDto>> = _localExperiences.asStateFlow()

    private val _selectedExperienceCategory = MutableStateFlow("")
    val selectedExperienceCategory: StateFlow<String> = _selectedExperienceCategory.asStateFlow()

    private var currentCategory: String? = null

    init {
        loadDestinations(null)
        loadLocalExperiences(null)
    }

    fun loadDestinations(category: String?) {
        viewModelScope.launch {
            val cached = when (category) {
                "Popular", null, "" -> destinationRepository.getCachedPopularDestinations()
                "Trending" -> destinationRepository.getCachedDestinations(tag = "Trending")
                else -> destinationRepository.getCachedDestinations(tag = category)
            }
            if (cached.isNotEmpty()) {
                _destinations.value = ApiResult.Success(cached)
            } else {
                _destinations.value = ApiResult.Loading
            }

            currentCategory = category
            try {
                val response = when (category) {
                    "Popular", null, "" -> destinationRepository.getPopularDestinations()
                    "Trending" -> destinationRepository.getDestinations(tag = "Trending")
                    else -> destinationRepository.getDestinations(tag = category)
                }
                if (response.isNotEmpty()) {
                    _destinations.value = ApiResult.Success(response)
                }
            } catch (e: Exception) {
                if (cached.isEmpty()) {
                    _destinations.value = ApiResult.Exception(e)
                }
            }
        }
    }

    fun loadLocalExperiences(category: String?) {
        _selectedExperienceCategory.value = category ?: ""
        viewModelScope.launch {
            try {
                val res = apiClient.getLocalExperiences("Jaipur")
                res.onSuccess { list ->
                    _localExperiences.value = if (!category.isNullOrBlank()) {
                        list.filter { it.category.contains(category, ignoreCase = true) }
                    } else {
                        list
                    }
                }
            } catch (e: Exception) {
                // Keep existing or fallback
            }
        }
    }
}
