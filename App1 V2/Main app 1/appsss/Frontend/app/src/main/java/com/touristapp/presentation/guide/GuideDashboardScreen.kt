package com.touristapp.presentation.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideDashboardScreen(
    onBackClick: () -> Unit,
    onCameraTranslationClick: () -> Unit,
    onPlaceDetectionClick: () -> Unit,
    onVoiceTranslationClick: () -> Unit,
    onTravelGuideClick: () -> Unit,
    onSavedItemsClick: () -> Unit,
    onHeritageLensClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Guide",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSavedItemsClick) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "Saved & History",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Explore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Your AI Travel Assistant",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Real-time Vision, Voice & Landmark Intelligence",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 0. AI Heritage Lens (SIH Major Differentiator)
            item {
                GuideFeatureCard(
                    title = "AI Heritage Lens",
                    subtitle = "Point camera at monuments for live history, architecture & audio narration",
                    icon = Icons.Filled.AccountBalance,
                    iconBgColor = Color(0xFFE65100),
                    badge = "Gemini Vision & TTS",
                    onClick = onHeritageLensClick
                )
            }

            // 1. Camera Translation Card
            item {
                GuideFeatureCard(
                    title = "Camera Translation",
                    subtitle = "Translate signs, menus and text instantly",
                    icon = Icons.Filled.PhotoCamera,
                    iconBgColor = Color(0xFF0284C7),
                    badge = "Instant OCR",
                    onClick = onCameraTranslationClick
                )
            }

            // 2. AI Place Detection Card
            item {
                GuideFeatureCard(
                    title = "AI Place Detection",
                    subtitle = "Point your camera at a place and discover it",
                    icon = Icons.Filled.LocationOn,
                    iconBgColor = Color(0xFF0D9488),
                    badge = "Visual AI",
                    onClick = onPlaceDetectionClick
                )
            }

            // 3. Live Voice Translation Card
            item {
                GuideFeatureCard(
                    title = "Live Voice Translation",
                    subtitle = "Speak naturally and translate conversations instantly",
                    icon = Icons.Filled.Mic,
                    iconBgColor = Color(0xFF7C3AED),
                    badge = "Two-Way Voice",
                    onClick = onVoiceTranslationClick
                )
            }

            // 4. Travel Guide Card
            item {
                GuideFeatureCard(
                    title = "Travel Guide",
                    subtitle = "Discover history, facts and useful information",
                    icon = Icons.Filled.TravelExplore,
                    iconBgColor = Color(0xFFD97706),
                    badge = "Knowledge Base",
                    onClick = onTravelGuideClick
                )
            }

            // 5. Recent / Saved Card
            item {
                GuideFeatureCard(
                    title = "Recent / Saved",
                    subtitle = "View previous translations and discoveries",
                    icon = Icons.Filled.AccessTime,
                    iconBgColor = Color(0xFF4B5563),
                    badge = "Offline Vault",
                    onClick = onSavedItemsClick
                )
            }
        }
    }
}

@Composable
private fun GuideFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Feature Icon
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = iconBgColor.copy(alpha = 0.15f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconBgColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = iconBgColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = iconBgColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
