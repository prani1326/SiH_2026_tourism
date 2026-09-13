package com.touristapp.presentation.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.ui.theme.*

data class DestinationItem(
    val id: String,
    val name: String,
    val landmark: String,
    val state: String,
    val icon: ImageVector
)

data class PreferenceItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelPreferenceOnboarding(
    onComplete: (Map<String, Any>) -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    // Comprehensive list of India's top tourist destinations with exact place & landmark names
    val destinations = remember {
        listOf(
            DestinationItem("agra", "Agra", "Taj Mahal & Agra Fort", "Uttar Pradesh", Icons.Filled.LocationOn),
            DestinationItem("jaipur", "Jaipur", "Hawa Mahal & Amer Fort", "Rajasthan", Icons.Filled.Castle),
            DestinationItem("goa", "Goa", "Calangute & Baga Beaches", "Goa", Icons.Filled.BeachAccess),
            DestinationItem("manali", "Manali", "Solang Valley & Rohtang", "Himachal Pradesh", Icons.Filled.Terrain),
            DestinationItem("kerala", "Kerala", "Munnar & Alleppey Backwaters", "Kerala", Icons.Filled.NaturePeople),
            DestinationItem("varanasi", "Varanasi", "Ganga Ghats & Kashi Vishwanath", "Uttar Pradesh", Icons.Filled.TempleHindu),
            DestinationItem("leh", "Leh Ladakh", "Pangong Lake & Khardung La", "Ladakh", Icons.Filled.Landscape),
            DestinationItem("udaipur", "Udaipur", "City Palace & Lake Pichola", "Rajasthan", Icons.Filled.Fort),
            DestinationItem("rishikesh", "Rishikesh", "River Rafting & Ganga Aarti", "Uttarakhand", Icons.Filled.Water),
            DestinationItem("kashmir", "Kashmir", "Dal Lake, Gulmarg & Pahalgam", "Jammu & Kashmir", Icons.Filled.AcUnit),
            DestinationItem("delhi", "Delhi", "India Gate, Red Fort & Qutub Minar", "National Capital", Icons.Filled.Apartment),
            DestinationItem("mumbai", "Mumbai", "Gateway of India & Marine Drive", "Maharashtra", Icons.Filled.DirectionsBoat),
            DestinationItem("darjeeling", "Darjeeling", "Tiger Hill & Tea Gardens", "West Bengal", Icons.Filled.Forest),
            DestinationItem("amritsar", "Amritsar", "Golden Temple & Wagah Border", "Punjab", Icons.Filled.Star),
            DestinationItem("ooty", "Ooty", "Botanical Garden & Nilgiri Hills", "Tamil Nadu", Icons.Filled.Park),
            DestinationItem("jaisalmer", "Jaisalmer", "Thar Desert & Golden Fort", "Rajasthan", Icons.Filled.WbSunny),
            DestinationItem("pondicherry", "Pondicherry", "French Colony & Promenade", "Puducherry", Icons.Filled.Villa),
            DestinationItem("hampi", "Hampi", "Virupaksha & Ancient Ruins", "Karnataka", Icons.Filled.AccountBalance),
            DestinationItem("meghalaya", "Meghalaya", "Shillong & Root Bridges", "Meghalaya", Icons.Filled.Cloud),
            DestinationItem("andaman", "Andaman", "Radhanagar Beach & Havelock", "Andaman & Nicobar", Icons.Filled.Sailing)
        )
    }

    val styles = remember {
        listOf(
            PreferenceItem("solo", "Solo Explorer", "Freedom, independent discovery & local vibe", Icons.Filled.Person),
            PreferenceItem("family", "Family Vacation", "Relaxed, comfortable & kid-friendly spots", Icons.Filled.FamilyRestroom),
            PreferenceItem("couple", "Romantic Couple", "Scenic getaways, privacy & candlelight dining", Icons.Filled.Favorite),
            PreferenceItem("friends", "Friends / Group", "Nightlife, road trips & group adventure", Icons.Filled.Groups),
            PreferenceItem("adventure", "Adventure & Trekking", "Thrills, hiking trails & camping", Icons.Filled.Hiking),
            PreferenceItem("luxury", "Luxury & Wellness", "5-star resorts, spa & premium experiences", Icons.Filled.Diamond),
            PreferenceItem("budget", "Budget Backpacker", "Hostels, smart savings & cultural immersion", Icons.Filled.Savings)
        )
    }

    val interests = remember {
        listOf(
            PreferenceItem("heritage", "Heritage & History", "Forts, palaces, temples & monuments", Icons.Filled.Castle),
            PreferenceItem("nature", "Nature & Wildlife", "National parks, waterfalls & scenic valleys", Icons.Filled.Forest),
            PreferenceItem("beaches", "Beaches & Coastal", "Sun, sand, water sports & beach shacks", Icons.Filled.BeachAccess),
            PreferenceItem("mountains", "Mountains & Snow", "Himalayan peaks, cool breezes & viewpoints", Icons.Filled.Terrain),
            PreferenceItem("food", "Food & Culinary", "Street food walks, local treats & fine dining", Icons.Filled.Restaurant),
            PreferenceItem("spiritual", "Spiritual & Peace", "Ghats, sacred temples & meditation centers", Icons.Filled.SelfImprovement),
            PreferenceItem("shopping", "Shopping & Bazaars", "Handicrafts, textiles & local markets", Icons.Filled.ShoppingBag),
            PreferenceItem("photography", "Photography Spots", "Sunrise vistas, architecture & landscapes", Icons.Filled.CameraAlt)
        )
    }

    var selectedDestinations by remember { mutableStateOf(setOf<String>()) }
    var selectedStyles by remember { mutableStateOf(setOf<String>()) }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        IconButton(
                            onClick = { step-- },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        Text(
                            "Personalize Your Trip",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.brandPillBackground
                    ) {
                        Text(
                            "Step $step of 3",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Indicator
                LinearProgressIndicator(
                    progress = { step / 3f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = TravelPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text(
                            "Skip for now",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    }

                    Button(
                        onClick = {
                            if (step < 3) {
                                step++
                            } else {
                                onComplete(
                                    mapOf(
                                        "destinations" to selectedDestinations.toList(),
                                        "styles" to selectedStyles.toList(),
                                        "interests" to selectedInterests.toList()
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .defaultMinSize(minWidth = 140.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (step < 3) "Continue" else "Finish & Explore",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                if (step < 3) Icons.AutoMirrored.Filled.ArrowForward else Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            when (step) {
                1 -> {
                    Text(
                        "Where do you want to go?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Select the destinations you'd love to visit across India.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(destinations, key = { it.id }) { item ->
                            val isSelected = selectedDestinations.contains(item.name)
                            DestinationCard(
                                item = item,
                                isSelected = isSelected,
                                onClick = {
                                    selectedDestinations = if (isSelected) {
                                        selectedDestinations - item.name
                                    } else {
                                        selectedDestinations + item.name
                                    }
                                }
                            )
                        }
                    }
                }

                2 -> {
                    Text(
                        "What is your travel style?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Help us personalize itineraries and recommendations for you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(1),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(styles, key = { it.id }) { item ->
                            val isSelected = selectedStyles.contains(item.title)
                            PreferenceListCard(
                                item = item,
                                isSelected = isSelected,
                                onClick = {
                                    selectedStyles = if (isSelected) {
                                        selectedStyles - item.title
                                    } else {
                                        selectedStyles + item.title
                                    }
                                }
                            )
                        }
                    }
                }

                3 -> {
                    Text(
                        "What are your interests?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Choose the activities and themes you enjoy the most.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(interests, key = { it.id }) { item ->
                            val isSelected = selectedInterests.contains(item.title)
                            InterestCard(
                                item = item,
                                isSelected = isSelected,
                                onClick = {
                                    selectedInterests = if (isSelected) {
                                        selectedInterests - item.title
                                    } else {
                                        selectedInterests + item.title
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(
    item: DestinationItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pillBg = MaterialTheme.colorScheme.brandPillBackground
    val surfaceBg = MaterialTheme.colorScheme.surface
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) pillBg else surfaceBg,
        label = "containerColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) TravelPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "borderColor"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) TravelPrimary else MaterialTheme.colorScheme.brandPillBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else TravelPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(TravelPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.landmark,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) TravelPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.state,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PreferenceListCard(
    item: PreferenceItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pillBg = MaterialTheme.colorScheme.brandPillBackground
    val surfaceBg = MaterialTheme.colorScheme.surface
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) pillBg else surfaceBg,
        label = "containerColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) TravelPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "borderColor"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) TravelPrimary else MaterialTheme.colorScheme.brandPillBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else TravelPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(TravelPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InterestCard(
    item: PreferenceItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pillBg = MaterialTheme.colorScheme.brandPillBackground
    val surfaceBg = MaterialTheme.colorScheme.surface
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) pillBg else surfaceBg,
        label = "containerColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) TravelPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "borderColor"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) TravelPrimary else MaterialTheme.colorScheme.brandPillBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else TravelPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(TravelPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
