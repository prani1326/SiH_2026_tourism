package com.touristapp.presentation.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.touristapp.data.models.DestinationDto
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.components.SearchBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: () -> Unit,
    onResultClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<DestinationDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val destinationRepository = remember { ServiceLocator.destinationRepository }

    val recentSearches = listOf("Taj Mahal", "Jaipur", "Goa Beaches", "Kerala Backwaters", "Varanasi")

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            isLoading = true
            results = destinationRepository.searchDestinations(query)
            isLoading = false
        } else {
            results = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Destinations") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                onSearch = { query = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (query.isEmpty()) {
                Text(
                    text = "Suggested Destinations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn {
                    items(recentSearches) { search ->
                        ListItem(
                            headlineContent = { Text(search) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    query = search
                                }
                                .padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            } else {
                Text(
                    text = if (isLoading) "Searching..." else "Results for '$query' (${results.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("No destinations found matching '$query'", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn {
                        items(results) { dest ->
                            ListItem(
                                headlineContent = { Text(dest.name, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("${dest.state}, ${dest.country} • ★ ${dest.rating}") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onResultClick(dest.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
