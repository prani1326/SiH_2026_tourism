package com.touristapp.presentation.guide

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.touristapp.data.models.HeritageAnalysisDto
import com.touristapp.data.models.HeritageMonumentDto
import com.touristapp.data.models.SavedPlaceItem
import com.touristapp.data.remote.BackendApiClient
import com.touristapp.data.repository.GuideRepository
import com.touristapp.data.speech.SpeechAndTtsManager
import com.touristapp.di.ServiceLocator
import com.touristapp.presentation.guide.components.CameraPreviewView
import com.touristapp.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HeritageLensUiState(
    val isAnalyzing: Boolean = false,
    val selectedLanguage: String = "en",
    val result: HeritageAnalysisDto? = null,
    val isSpeaking: Boolean = false,
    val flashEnabled: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class HeritageLensViewModel(
    private val apiClient: BackendApiClient = ServiceLocator.backendApiClient,
    private val speechManager: SpeechAndTtsManager = ServiceLocator.speechAndTtsManager,
    private val guideRepository: GuideRepository = ServiceLocator.guideRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HeritageLensUiState())
    val uiState: StateFlow<HeritageLensUiState> = _uiState.asStateFlow()

    fun toggleFlash() {
        _uiState.update { it.copy(flashEnabled = !it.flashEnabled) }
    }

    fun setLanguage(lang: String) {
        _uiState.update { it.copy(selectedLanguage = lang) }
        val currentResult = _uiState.value.result
        if (currentResult != null) {
            analyzeMonument(currentResult.monument.name)
        }
    }

    fun analyzeMonument(hint: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }
            try {
                val monumentName = hint?.takeIf { it.isNotBlank() } ?: "Amber Fort"
                val response = apiClient.analyzeHeritage(monumentHint = monumentName)
                response.onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            result = data,
                            isSaved = false
                        )
                    }
                }.onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            errorMessage = err.message ?: "Heritage analysis unavailable offline"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = "Error recognizing heritage: ${e.message}"
                    )
                }
            }
        }
    }

    fun playAudioNarration() {
        val monument = _uiState.value.result?.monument ?: return
        val lang = _uiState.value.selectedLanguage
        val script = if (lang == "hi" && monument.narration_script_hi.isNotBlank()) {
            monument.narration_script_hi
        } else {
            monument.narration_script_en.ifBlank { monument.historical_context }
        }
        speechManager.speak(script, lang)
        _uiState.update { it.copy(isSpeaking = true) }
    }

    fun stopAudio() {
        speechManager.stopSpeaking()
        _uiState.update { it.copy(isSpeaking = false) }
    }

    fun saveToSavedPlaces() {
        val res = _uiState.value.result?.monument ?: return
        viewModelScope.launch {
            val place = SavedPlaceItem(
                placeName = res.name,
                location = res.location,
                description = res.historical_context,
                category = "Heritage Site"
            )
            val success = guideRepository.savePlace(place)
            if (success) {
                _uiState.update { it.copy(isSaved = true) }
            }
        }
    }

    fun clearResult() {
        stopAudio()
        _uiState.update {
            it.copy(
                result = null,
                isSaved = false,
                errorMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.stopSpeaking()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeritageLensScreen(
    onBackClick: () -> Unit,
    viewModel: HeritageLensViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = null,
                            tint = PrimaryOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Heritage Lens", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFlash() }) {
                        Icon(
                            imageVector = if (uiState.flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Flash",
                            tint = if (uiState.flashEnabled) PrimaryOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Language Selection Bar
                LanguageSelectionRow(
                    selectedLang = uiState.selectedLanguage,
                    onLangSelected = { viewModel.setLanguage(it) }
                )

                // Quick Monument Target Chips
                QuickMonumentChips(
                    onMonumentSelected = { viewModel.analyzeMonument(it) }
                )

                // Main Content (Camera Preview or Result Details)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (hasCameraPermission) {
                        CameraPreviewView(
                            flashEnabled = uiState.flashEnabled,
                            viewfinderSubtitle = "Point camera at monument, architecture, or inscription",
                            onFlashToggle = { viewModel.toggleFlash() },
                            onCapture = { text, _ ->
                                viewModel.analyzeMonument(text)
                            },
                            isProcessing = uiState.isAnalyzing
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = PrimaryOrange,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Camera Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Camera access allows AI Heritage Lens to analyze monuments and historical structures in real time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                            ) {
                                Text("Grant Camera Access", color = Color.White)
                            }
                        }
                    }

                    // Result Card Overlay
                    val res = uiState.result
                    if (res != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        ) {
                            HeritageResultCard(
                                result = res,
                                isSpeaking = uiState.isSpeaking,
                                isSaved = uiState.isSaved,
                                onListenClick = {
                                    if (uiState.isSpeaking) viewModel.stopAudio() else viewModel.playAudioNarration()
                                },
                                onSaveClick = {
                                    viewModel.saveToSavedPlaces()
                                    Toast.makeText(context, "Saved to your Places!", Toast.LENGTH_SHORT).show()
                                },
                                onCloseClick = { viewModel.clearResult() }
                            )
                        }
                    }
                }
            }

            // Error banner
            if (uiState.errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.clearResult() }) {
                            Text("DISMISS", color = Color.White)
                        }
                    }
                ) {
                    Text(uiState.errorMessage ?: "")
                }
            }
        }
    }
}

@Composable
private fun LanguageSelectionRow(
    selectedLang: String,
    onLangSelected: (String) -> Unit
) {
    val languages = listOf(
        "en" to "English",
        "hi" to "हिन्दी (Hindi)",
        "ta" to "தமிழ் (Tamil)",
        "bn" to "বাংলা (Bengali)",
        "mr" to "मराठी (Marathi)"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(languages) { (code, label) ->
            val isSelected = code == selectedLang
            FilterChip(
                selected = isSelected,
                onClick = { onLangSelected(code) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryOrange.copy(alpha = 0.15f),
                    selectedLabelColor = PrimaryOrange
                )
            )
        }
    }
}

@Composable
private fun QuickMonumentChips(
    onMonumentSelected: (String) -> Unit
) {
    val quickMonuments = listOf(
        "Amber Fort",
        "Hawa Mahal",
        "City Palace",
        "Jantar Mantar",
        "Qutub Minar",
        "Red Fort",
        "Taj Mahal"
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            text = "Try Monument / Heritage Scan:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickMonuments) { name ->
                AssistChip(
                    onClick = { onMonumentSelected(name) },
                    label = { Text(name, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryOrange
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun HeritageResultCard(
    result: HeritageAnalysisDto,
    isSpeaking: Boolean,
    isSaved: Boolean,
    onListenClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val monument = result.monument
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = monument.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val isHighConfidence = result.match_confidence >= 0.8
                        val confidenceText = if (isHighConfidence) {
                            "Verified AI Match (${(result.match_confidence * 100).toInt()}%)"
                        } else {
                            "Possible Match (${(result.match_confidence * 100).toInt()}%)"
                        }
                        val badgeBg = if (isHighConfidence) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        val badgeColor = if (isHighConfidence) Color(0xFF2E7D32) else Color(0xFFEF6C00)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = confidenceText,
                                color = badgeColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(onClick = onCloseClick) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Specs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpecPill(
                        label = "Built",
                        value = monument.built_period,
                        modifier = Modifier.weight(1f)
                    )
                    SpecPill(
                        label = "Architecture",
                        value = monument.architectural_style,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Historical Context
            item {
                Text(
                    text = "Historical Context",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = monument.historical_context,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Did You Know
            if (monument.did_you_know.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Did You Know?",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryOrange
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            monument.did_you_know.forEach { fact ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("• ", fontWeight = FontWeight.Bold, color = PrimaryOrange)
                                    Text(
                                        text = fact,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onListenClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSpeaking) Color(0xFFD32F2F) else PrimaryOrange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSpeaking) "Stop Audio" else "Listen AI Narration", color = Color.White, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onSaveClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isSaved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.BookmarkAdded else Icons.Filled.BookmarkBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
