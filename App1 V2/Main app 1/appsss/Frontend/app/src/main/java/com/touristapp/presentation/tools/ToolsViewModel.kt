package com.touristapp.presentation.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.remote.ApiResult
import com.touristapp.data.remote.model.*
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ToolsViewModel : ViewModel() {

    private val toolsRepository = ServiceLocator.toolsRepository

    private val _simPlans = MutableStateFlow<ApiResult<List<SimPlanDto>>>(ApiResult.Loading)
    val simPlans: StateFlow<ApiResult<List<SimPlanDto>>> = _simPlans

    private val _visaDocs = MutableStateFlow<ApiResult<List<VisaDocumentDto>>>(ApiResult.Loading)
    val visaDocs: StateFlow<ApiResult<List<VisaDocumentDto>>> = _visaDocs

    private val _translatedResult = MutableStateFlow<String?>(null)
    val translatedResult: StateFlow<String?> = _translatedResult

    private val _menuScanResult = MutableStateFlow<MenuScanResponseDto?>(null)
    val menuScanResult: StateFlow<MenuScanResponseDto?> = _menuScanResult

    private val _insurancePolicies = MutableStateFlow<ApiResult<List<InsurancePolicyDto>>>(ApiResult.Loading)
    val insurancePolicies: StateFlow<ApiResult<List<InsurancePolicyDto>>> = _insurancePolicies

    private val _preparationChecklist = MutableStateFlow<ApiResult<PreparationChecklistDto>>(ApiResult.Loading)
    val preparationChecklist: StateFlow<ApiResult<PreparationChecklistDto>> = _preparationChecklist

    private val _transportComparison = MutableStateFlow<ApiResult<TransportComparisonResponseDto>>(ApiResult.Loading)
    val transportComparison: StateFlow<ApiResult<TransportComparisonResponseDto>> = _transportComparison

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    init {
        loadSimPlans()
        loadVisaDocs()
        loadInsurancePolicies()
        loadPreparationChecklist("Jaipur")
        loadTransportComparison("Agra")
    }

    fun loadSimPlans() {
        viewModelScope.launch {
            _simPlans.value = ApiResult.Loading
            val res = toolsRepository.getSimPlans()
            res.onSuccess { _simPlans.value = ApiResult.Success(it) }
                .onFailure { _simPlans.value = ApiResult.Exception(it) }
        }
    }

    fun loadVisaDocs() {
        viewModelScope.launch {
            _visaDocs.value = ApiResult.Loading
            val res = toolsRepository.getVisaDocuments()
            res.onSuccess { _visaDocs.value = ApiResult.Success(it) }
                .onFailure { _visaDocs.value = ApiResult.Exception(it) }
        }
    }

    fun loadInsurancePolicies() {
        viewModelScope.launch {
            _insurancePolicies.value = ApiResult.Loading
            val res = toolsRepository.getInsurancePolicies()
            res.onSuccess { _insurancePolicies.value = ApiResult.Success(it) }
                .onFailure { _insurancePolicies.value = ApiResult.Exception(it) }
        }
    }

    fun purchaseInsurance(policyId: String) {
        viewModelScope.launch {
            val res = toolsRepository.purchaseInsurancePolicy(policyId)
            res.onSuccess { policyNum ->
                _statusMessage.value = "Insurance Policy Issued! Policy #: $policyNum"
            }.onFailure { e ->
                _statusMessage.value = "Insurance purchase failed: ${e.message}"
            }
        }
    }

    fun loadPreparationChecklist(destination: String) {
        viewModelScope.launch {
            _preparationChecklist.value = ApiResult.Loading
            val res = toolsRepository.getPreparationChecklist(destination)
            res.onSuccess { _preparationChecklist.value = ApiResult.Success(it) }
                .onFailure { _preparationChecklist.value = ApiResult.Exception(it) }
        }
    }

    fun loadTransportComparison(city: String) {
        viewModelScope.launch {
            _transportComparison.value = ApiResult.Loading
            val res = toolsRepository.compareTransport(city)
            res.onSuccess { _transportComparison.value = ApiResult.Success(it) }
                .onFailure { _transportComparison.value = ApiResult.Exception(it) }
        }
    }

    fun purchaseSim(planId: String) {
        viewModelScope.launch {
            val res = toolsRepository.purchaseSimPlan(planId)
            res.onSuccess { code ->
                _statusMessage.value = "eSIM Activated! Activation Code: $code"
            }.onFailure { e ->
                _statusMessage.value = "Failed to purchase eSIM: ${e.message}"
            }
        }
    }

    fun translate(text: String, targetLang: String) {
        viewModelScope.launch {
            val res = toolsRepository.translateText(text, targetLang)
            res.onSuccess {
                _translatedResult.value = it.translatedText
            }.onFailure {
                _statusMessage.value = "Translation error: ${it.message}"
            }
        }
    }

    fun scanMenu(menuText: String, diet: String) {
        viewModelScope.launch {
            val res = toolsRepository.scanMenu(menuText, diet)
            res.onSuccess {
                _menuScanResult.value = it
            }.onFailure {
                _statusMessage.value = "Menu scan error: ${it.message}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}

