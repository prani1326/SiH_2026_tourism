package com.travellikepro.travelvendor.ui.kyc

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travellikepro.travelvendor.data.model.KycResponseData
import com.travellikepro.travelvendor.data.model.KycSubmitRequest
import com.travellikepro.travelvendor.data.repository.KycRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class KycUiState {
    object Loading : KycUiState()
    data class Success(val data: KycResponseData) : KycUiState()
    data class Error(val message: String) : KycUiState()
}

class KycViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KycRepository(application)

    private val _uiState = MutableStateFlow<KycUiState>(KycUiState.Loading)
    val uiState: StateFlow<KycUiState> = _uiState

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess

    private val _uploadMessage = MutableStateFlow<String?>(null)
    val uploadMessage: StateFlow<String?> = _uploadMessage

    init {
        loadKycStatus()
    }

    fun loadKycStatus() {
        viewModelScope.launch {
            _uiState.value = KycUiState.Loading
            try {
                val response = repository.getKycStatus()
                if (response.success && response.data != null) {
                    _uiState.value = KycUiState.Success(response.data)
                } else {
                    _uiState.value = KycUiState.Error(response.message ?: "Failed to load KYC status")
                }
            } catch (e: Exception) {
                _uiState.value = KycUiState.Error(e.localizedMessage ?: "Network error loading KYC")
            }
        }
    }

    fun submitKycForm(request: KycSubmitRequest) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _submitSuccess.value = false
            try {
                val response = repository.submitKyc(request)
                if (response.success) {
                    _submitSuccess.value = true
                    loadKycStatus()
                } else {
                    _uiState.value = KycUiState.Error(response.message ?: "Submission failed")
                }
            } catch (e: Exception) {
                _uiState.value = KycUiState.Error(e.localizedMessage ?: "Network error submitting KYC")
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun uploadDocument(file: File, docType: String) {
        viewModelScope.launch {
            try {
                _uploadMessage.value = "Uploading $docType..."
                repository.uploadDocument(file, docType)
                _uploadMessage.value = "Successfully uploaded $docType!"
                loadKycStatus()
            } catch (e: Exception) {
                _uploadMessage.value = "Failed to upload document: ${e.localizedMessage}"
            }
        }
    }

    fun clearUploadMessage() {
        _uploadMessage.value = null
    }
}
