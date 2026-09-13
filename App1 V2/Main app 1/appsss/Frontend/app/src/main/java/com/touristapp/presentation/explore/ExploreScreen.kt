package com.touristapp.presentation.explore

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.touristapp.data.models.DestinationDto
import com.touristapp.data.remote.ApiResult
import com.touristapp.presentation.explore.components.LocalExperiencesSection
import com.touristapp.ui.components.DestinationCard
import com.touristapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onDestinationClick: (String) -> Unit,
    viewModel: ExploreViewModel = viewModel()
) {
    val context = LocalContext.current
    val categories = listOf(
        "Popular", "Trending", "Heritage", "Beaches", "Mountains", "Adventure", "Food", "Culture", "Spiritual", "Shopping"
    )
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var searchQuery by remember { mutableStateOf("") }
    var minRating by remember { mutableStateOf(0f) }

    val destinationsResult by viewModel.destinations.collectAsState()
    val localExperiences by viewModel.localExperiences.collectAsState()
    val selectedExperienceCategory by viewModel.selectedExperienceCategory.collectAsState()

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(title = { Text("Explore Destinations", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search destinations...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TravelPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Category tabs
            PrimaryScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TravelPrimary
            ) {
                categories.forEachIndexed { _, category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = category
                            viewModel.loadDestinations(category)
                        },
                        text = { Text(category) }
                    )
                }
            }

            // Rating filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0f to "Any Rating", 4f to "4★+", 4.5f to "4.5★+").forEach { (rating, label) ->
                    FilterChip(
                        selected = minRating == rating,
                        onClick = { minRating = rating },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TravelPrimary,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        )
                    )
                }
            }

            // Content
            when (val res = destinationsResult) {
                is ApiResult.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TravelPrimary)
                    }
                }
                is ApiResult.Error, is ApiResult.Exception -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Failed to load destinations", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadDestinations(if (selectedCategory == "All") null else selectedCategory) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is ApiResult.Success -> {
                    val allDests = res.data

                    // Filter by search and min rating
                    val filtered = allDests.filter { dest ->
                        (searchQuery.isEmpty() || dest.name.contains(searchQuery, ignoreCase = true) ||
                                dest.state?.contains(searchQuery, ignoreCase = true) == true) &&
                                (dest.rating >= minRating)
                    }

                    val trending = filtered.sortedByDescending { it.rating }.take(5)

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No destinations found.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Local Experiences Section (spans both columns)
                            if (localExperiences.isNotEmpty() && searchQuery.isEmpty()) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                    LocalExperiencesSection(
                                        experiences = localExperiences,
                                        selectedCategory = selectedExperienceCategory,
                                        onCategorySelected = { viewModel.loadLocalExperiences(it) },
                                        onExperienceClick = { exp ->
                                            Toast.makeText(
                                                context,
                                                "${exp.title} by ${exp.host_name} (₹${exp.price_inr})",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }
                            }

                            // Trending section header (spans both columns)
                            if (trending.isNotEmpty() && searchQuery.isEmpty()) {
                                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                                    Column {
                                        Text(
                                            "🔥 Trending",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TravelPrimary,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            contentPadding = PaddingValues(horizontal = 0.dp)
                                        ) {
                                            items(trending) { dest ->
                                                DestinationCard(
                                                    title = dest.name,
                                                    imageUrl = dest.getDisplayImageUrl(),
                                                    tag = dest.tags.firstOrNull() ?: "Explore",
                                                    descriptor = dest.knownFor ?: dest.description,
                                                    stat = dest.rating.toString(),
                                                    onClick = { onDestinationClick(dest.id) }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "All Destinations",
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }
                            }

                            items(filtered) { dest ->
                                DestinationCard(
                                    title = dest.name,
                                    imageUrl = dest.getDisplayImageUrl(),
                                    tag = dest.tags.firstOrNull() ?: "Explore",
                                    descriptor = dest.knownFor ?: dest.description,
                                    stat = dest.rating.toString(),
                                    onClick = { onDestinationClick(dest.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
