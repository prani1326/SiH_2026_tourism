package com.touristapp.presentation.guide

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.touristapp.data.models.SupportedLanguages
import com.touristapp.data.models.VoiceChatMessage
import com.touristapp.data.models.VoiceSender
import com.touristapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTranslationScreen(
    onBackClick: () -> Unit,
    viewModel: GuideViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.voiceTransState.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    var showTravelerLangDialog by remember { mutableStateOf(false) }
    var showLocalLangDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LaunchedEffect(uiState.conversationMessages.size) {
        if (uiState.conversationMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.conversationMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Live Voice Translation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Two-Way Conversation Mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearVoiceConversation() }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Dual Language Header Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Traveler Language
                    val travelerLangItem = SupportedLanguages.find { it.code == uiState.travelerLang } ?: SupportedLanguages[1]
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showTravelerLangDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Traveler (You)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(travelerLangItem.flagEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(travelerLangItem.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Swap Button
                    IconButton(
                        onClick = { viewModel.swapConversationLanguages() },
                        modifier = Modifier
                            .background(TravelPrimary.copy(alpha = 0.15f), CircleShape)
                            .size(38.dp)
                    ) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap Languages", tint = TravelPrimary)
                    }

                    // Local Person Language
                    val localLangItem = SupportedLanguages.find { it.code == uiState.localLang } ?: SupportedLanguages[2]
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showLocalLangDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("Local Person", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(localLangItem.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(localLangItem.flagEmoji, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Error or Permission Warning Banner
            uiState.errorMessage?.let { err ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Conversation Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.conversationMessages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TravelPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = TravelPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Ready for Live Voice Translation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap either microphone below to speak.\nAI will transcribe, translate, and speak the answer aloud automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                items(uiState.conversationMessages) { message ->
                    VoiceBubble(message = message, onPlayTts = { text, lang ->
                        viewModel.playTranslationAudio(text, lang)
                    })
                }

                // Live Active Transcription indicator
                if (uiState.currentSpokenText.isNotBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Listening: \"${uiState.currentSpokenText}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Dual Push-to-Talk Microphones Area
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Traveler Mic Button
                    val isTravelerActive = isListening && uiState.activeSpeaker == VoiceSender.TRAVELER
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isTravelerActive) Color(0xFFEF4444) else TravelPrimary
                                )
                                .clickable {
                                    if (!hasAudioPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        if (isTravelerActive) {
                                            viewModel.stopVoiceListening()
                                        } else {
                                            viewModel.startVoiceListening(VoiceSender.TRAVELER)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isTravelerActive) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = "Speak as Traveler",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isTravelerActive) "Listening..." else "Traveler (${uiState.travelerLang.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isTravelerActive) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Divider
                    VerticalDivider(modifier = Modifier.height(48.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // Local Person Mic Button
                    val isLocalActive = isListening && uiState.activeSpeaker == VoiceSender.LOCAL
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isLocalActive) Color(0xFFEF4444) else Color(0xFF0D9488)
                                )
                                .clickable {
                                    if (!hasAudioPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        if (isLocalActive) {
                                            viewModel.stopVoiceListening()
                                        } else {
                                            viewModel.startVoiceListening(VoiceSender.LOCAL)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isLocalActive) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = "Speak as Local",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isLocalActive) "Listening..." else "Local (${uiState.localLang.uppercase()})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLocalActive) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // Language Selector Dialogs
    if (showTravelerLangDialog) {
        AlertDialog(
            onDismissRequest = { showTravelerLangDialog = false },
            title = { Text("Select Traveler Language", fontWeight = FontWeight.Bold) },
            text = {
                val langs = SupportedLanguages.filter { it.code != "auto" }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(langs) { lang ->
                        FilterChip(
                            selected = uiState.travelerLang == lang.code,
                            onClick = {
                                viewModel.setTravelerLang(lang.code)
                                showTravelerLangDialog = false
                            },
                            label = { Text("${lang.flagEmoji} ${lang.name}") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTravelerLangDialog = false }) { Text("Done") }
            }
        )
    }

    if (showLocalLangDialog) {
        AlertDialog(
            onDismissRequest = { showLocalLangDialog = false },
            title = { Text("Select Local Language", fontWeight = FontWeight.Bold) },
            text = {
                val langs = SupportedLanguages.filter { it.code != "auto" }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(langs) { lang ->
                        FilterChip(
                            selected = uiState.localLang == lang.code,
                            onClick = {
                                viewModel.setLocalLang(lang.code)
                                showLocalLangDialog = false
                            },
                            label = { Text("${lang.flagEmoji} ${lang.name}") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocalLangDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
private fun VoiceBubble(
    message: VoiceChatMessage,
    onPlayTts: (text: String, lang: String) -> Unit
) {
    val isTraveler = message.sender == VoiceSender.TRAVELER
    val alignment = if (isTraveler) Alignment.CenterStart else Alignment.CenterEnd
    val containerColor = if (isTraveler) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isTraveler) 4.dp else 18.dp,
                bottomEnd = if (isTraveler) 18.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isTraveler) "Traveler" else "Local Person",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isTraveler) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                    IconButton(
                        onClick = { onPlayTts(message.translatedText, message.targetLang) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Play Translation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Original Spoken Text
                Text(
                    text = message.originalText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Translated Text
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = message.translatedText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
