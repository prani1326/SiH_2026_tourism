package com.touristapp.presentation.guide

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.touristapp.presentation.guide.components.CameraPreviewView
import com.touristapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraTranslationScreen(
    onBackClick: () -> Unit,
    viewModel: GuideViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.cameraTransState.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

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

    var showSourceLangDialog by remember { mutableStateOf(false) }
    var showTargetLangDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreviewView(
                flashEnabled = uiState.flashEnabled,
                viewfinderSubtitle = "Point camera at signs, menus or foreign text",
                onFlashToggle = { viewModel.toggleCameraFlash() },
                onCapture = { text, _ ->
                    viewModel.processCapturedText(text ?: "Bienvenue à Paris\nRestaurant & Bar")
                },
                isProcessing = uiState.isAnalyzing
            )
        } else {
            // Permission Denied View
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
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grant camera permission to instantly scan and translate foreign text, signs, and menus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top Controls (Back, Flash, Title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .size(42.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "Camera Translation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            IconButton(
                onClick = { viewModel.toggleCameraFlash() },
                modifier = Modifier
                    .background(
                        if (uiState.flashEnabled) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .size(42.dp)
            ) {
                Icon(
                    imageVector = if (uiState.flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }
        }

        // Language Selector Bar (Floated near top under header)
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .offset(y = 60.dp)
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.75f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Source Language
                val srcLangItem = SupportedLanguages.find { it.code == uiState.sourceLang } ?: SupportedLanguages.first()
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.clickable { showSourceLangDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(srcLangItem.flagEmoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(srcLangItem.name, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // Swap Button
                IconButton(
                    onClick = { viewModel.swapCameraLanguages() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap", tint = Color.White)
                }

                // Target Language
                val tgtLangItem = SupportedLanguages.find { it.code == uiState.targetLang } ?: SupportedLanguages[1]
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TravelPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.clickable { showTargetLangDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tgtLangItem.flagEmoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tgtLangItem.name, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Bottom Capture / Action Controls
        if (uiState.translatedText == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isAnalyzing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reading & Translating...", color = Color.White, fontWeight = FontWeight.SemiBold)
                } else {
                    // Shutter Capture Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                            .clickable {
                                viewModel.processCapturedText("Bienvenue à Paris\nRestaurant & Bar")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap to Translate", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Translation Result Bottom Sheet Card
        AnimatedVisibility(
            visible = uiState.translatedText != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Detected: ${uiState.detectedSourceLang?.uppercase() ?: "AUTO"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.clearCameraTranslation() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Original Text
                    Text("Original Text", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = uiState.originalText ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Translated Text
                    Text("Translation (${uiState.targetLang.uppercase()})", style = MaterialTheme.typography.labelSmall, color = TravelPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = uiState.translatedText ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row: Listen, Copy, Save, Share, Retake
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Listen (TTS)
                        IconButton(
                            onClick = {
                                uiState.translatedText?.let {
                                    viewModel.playTranslationAudio(it, uiState.targetLang)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.VolumeUp else Icons.Filled.VolumeMute,
                                contentDescription = "Listen",
                                tint = TravelPrimary
                            )
                        }

                        // Copy
                        IconButton(
                            onClick = {
                                uiState.translatedText?.let { text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Translation", text))
                                    Toast.makeText(context, "Translation copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        // Save to Firebase
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentTranslation()
                                Toast.makeText(context, "Saved to Translation History", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.isSaved) Icons.Filled.BookmarkAdded else Icons.Filled.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (uiState.isSaved) TravelSuccess else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Share
                        IconButton(
                            onClick = {
                                uiState.translatedText?.let { text ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Original: ${uiState.originalText}\nTranslation: $text")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Translation"))
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
                        }

                        // Retake
                        Button(
                            onClick = { viewModel.clearCameraTranslation() },
                            colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retake", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Source Language Selection Dialog
    if (showSourceLangDialog) {
        AlertDialog(
            onDismissRequest = { showSourceLangDialog = false },
            title = { Text("Select Source Language", fontWeight = FontWeight.Bold) },
            text = {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SupportedLanguages) { lang ->
                        FilterChip(
                            selected = uiState.sourceLang == lang.code,
                            onClick = {
                                viewModel.setCameraSourceLang(lang.code)
                                showSourceLangDialog = false
                            },
                            label = { Text("${lang.flagEmoji} ${lang.name}") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSourceLangDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Target Language Selection Dialog
    if (showTargetLangDialog) {
        AlertDialog(
            onDismissRequest = { showTargetLangDialog = false },
            title = { Text("Select Target Language", fontWeight = FontWeight.Bold) },
            text = {
                val targets = SupportedLanguages.filter { it.code != "auto" }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(targets) { lang ->
                        FilterChip(
                            selected = uiState.targetLang == lang.code,
                            onClick = {
                                viewModel.setCameraTargetLang(lang.code)
                                showTargetLangDialog = false
                            },
                            label = { Text("${lang.flagEmoji} ${lang.name}") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTargetLangDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
