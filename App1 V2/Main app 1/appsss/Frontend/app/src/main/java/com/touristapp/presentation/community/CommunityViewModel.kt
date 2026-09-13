package com.touristapp.presentation.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.remote.ApiResult
import com.touristapp.data.remote.model.CommunityForumDto
import com.touristapp.data.remote.model.CommunityPostDto
import com.touristapp.data.remote.model.CreatorItineraryDto
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CommunityViewModel : ViewModel() {

    private val communityRepository = ServiceLocator.communityRepository
    private val tripRepository = ServiceLocator.tripRepository

    private val _forums = MutableStateFlow<ApiResult<List<CommunityForumDto>>>(ApiResult.Loading)
    val forums: StateFlow<ApiResult<List<CommunityForumDto>>> = _forums

    private val _posts = MutableStateFlow<ApiResult<List<CommunityPostDto>>>(ApiResult.Loading)
    val posts: StateFlow<ApiResult<List<CommunityPostDto>>> = _posts

    private val _creatorItineraries = MutableStateFlow<ApiResult<List<CreatorItineraryDto>>>(ApiResult.Loading)
    val creatorItineraries: StateFlow<ApiResult<List<CreatorItineraryDto>>> = _creatorItineraries

    private val _selectedForumId = MutableStateFlow<String?>(null)
    val selectedForumId: StateFlow<String?> = _selectedForumId

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage

    init {
        loadData()
    }

    fun loadData() {
        loadForums()
        loadPosts()
        loadCreatorItineraries()
    }

    fun selectForum(forumId: String?) {
        _selectedForumId.value = forumId
        loadPosts(forumId)
    }

    fun loadForums() {
        viewModelScope.launch {
            _forums.value = ApiResult.Loading
            val res = communityRepository.getForums()
            res.onSuccess { _forums.value = ApiResult.Success(it) }
                .onFailure { _forums.value = ApiResult.Exception(it) }
        }
    }

    fun loadPosts(forumId: String? = _selectedForumId.value) {
        viewModelScope.launch {
            _posts.value = ApiResult.Loading
            val res = communityRepository.getPosts(forumId)
            res.onSuccess { _posts.value = ApiResult.Success(it) }
                .onFailure { _posts.value = ApiResult.Exception(it) }
        }
    }

    fun loadCreatorItineraries() {
        viewModelScope.launch {
            _creatorItineraries.value = ApiResult.Loading
            val res = communityRepository.getCreatorItineraries()
            res.onSuccess { _creatorItineraries.value = ApiResult.Success(it) }
                .onFailure { _creatorItineraries.value = ApiResult.Exception(it) }
        }
    }

    fun createPost(title: String, content: String) {
        val forumId = _selectedForumId.value ?: "forum-heritage-lovers"
        viewModelScope.launch {
            val res = communityRepository.createPost(forumId, title, content)
            res.onSuccess {
                _actionMessage.value = "Post shared successfully!"
                loadPosts(forumId)
            }.onFailure {
                _actionMessage.value = "Could not publish post: ${it.message}"
            }
        }
    }

    fun copyCreatorItinerary(creatorItineraryId: String, onTripCopied: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val res = communityRepository.copyCreatorItinerary(creatorItineraryId)
            res.onSuccess { tripId ->
                _actionMessage.value = "Itinerary copied to your trips!"
                onTripCopied?.invoke(tripId)
            }.onFailure {
                _actionMessage.value = "Failed to copy itinerary."
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }
}
