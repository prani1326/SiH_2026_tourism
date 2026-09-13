package com.touristapp.presentation.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.touristapp.data.remote.ApiResult
import com.touristapp.data.remote.model.CommunityForumDto
import com.touristapp.data.remote.model.CommunityPostDto
import com.touristapp.data.remote.model.CreatorItineraryDto
import com.touristapp.ui.components.EmptyState
import com.touristapp.ui.components.ErrorState
import com.touristapp.ui.components.SkeletonLoader
import com.touristapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onItineraryClick: ((String) -> Unit)? = null,
    viewModel: CommunityViewModel = viewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Discussions", "Forums", "Creator Guides")

    val forumsResult by viewModel.forums.collectAsState()
    val postsResult by viewModel.posts.collectAsState()
    val creatorsResult by viewModel.creatorItineraries.collectAsState()
    val actionMsg by viewModel.actionMessage.collectAsState()

    var showCreatePostDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMsg) {
        actionMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Traveler Community",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Connect, share verified tips & copy itineraries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { showCreatePostDialog = true },
                    containerColor = TravelPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New Post")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            // Modern Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = TravelPrimary
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> PostsTabContent(
                    postsResult = postsResult,
                    onRetry = { viewModel.loadPosts() }
                )
                1 -> ForumsTabContent(
                    forumsResult = forumsResult,
                    onSelectForum = { forumId ->
                        viewModel.selectForum(forumId)
                        selectedTabIndex = 0
                    },
                    onRetry = { viewModel.loadForums() }
                )
                2 -> CreatorGuidesTabContent(
                    creatorsResult = creatorsResult,
                    onCopyItinerary = { id -> viewModel.copyCreatorItinerary(id, onItineraryClick) },
                    onRetry = { viewModel.loadCreatorItineraries() }
                )
            }
        }
    }

    if (showCreatePostDialog) {
        CreatePostDialog(
            onDismiss = { showCreatePostDialog = false },
            onPost = { title, content ->
                viewModel.createPost(title, content)
                showCreatePostDialog = false
            }
        )
    }
}

@Composable
private fun PostsTabContent(
    postsResult: ApiResult<List<CommunityPostDto>>,
    onRetry: () -> Unit
) {
    when (postsResult) {
        is ApiResult.Loading -> {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(100.dp)) }
            }
        }
        is ApiResult.Exception -> {
            ErrorState(message = postsResult.e.message ?: "Could not load discussions", onRetry = onRetry)
        }
        is ApiResult.Error -> {
            ErrorState(message = postsResult.message, onRetry = onRetry)
        }
        is ApiResult.Success -> {
            val posts = postsResult.data
            if (posts.isEmpty()) {
                EmptyState(
                    title = "No discussions yet",
                    message = "Be the first traveler to start a conversation!"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(posts) { post ->
                        PostCard(post = post)
                    }
                }
            }
        }
    }
}

@Composable
private fun PostCard(post: CommunityPostDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.brandPillBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = TravelPrimary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = post.userName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = post.createdAt ?: "Recently",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${post.likesCount} Helpful", style = MaterialTheme.typography.labelSmall, color = TravelPrimary)

                Spacer(modifier = Modifier.width(16.dp))

                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${post.commentsCount} replies", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun ForumsTabContent(
    forumsResult: ApiResult<List<CommunityForumDto>>,
    onSelectForum: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (forumsResult) {
        is ApiResult.Loading -> {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(140.dp)) }
            }
        }
        is ApiResult.Exception -> {
            ErrorState(message = forumsResult.e.message ?: "Could not load forums", onRetry = onRetry)
        }
        is ApiResult.Error -> {
            ErrorState(message = forumsResult.message, onRetry = onRetry)
        }
        is ApiResult.Success -> {
            val forums = forumsResult.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(forums) { forum ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectForum(forum.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            if (!forum.coverImage.isNullOrBlank()) {
                                AsyncImage(
                                    model = forum.coverImage,
                                    contentDescription = forum.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = forum.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = forum.description ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Groups, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${forum.memberCount} members", style = MaterialTheme.typography.labelSmall, color = TravelPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatorGuidesTabContent(
    creatorsResult: ApiResult<List<CreatorItineraryDto>>,
    onCopyItinerary: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (creatorsResult) {
        is ApiResult.Loading -> {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(160.dp)) }
            }
        }
        is ApiResult.Exception -> {
            ErrorState(message = creatorsResult.e.message ?: "Could not load creator itineraries", onRetry = onRetry)
        }
        is ApiResult.Error -> {
            ErrorState(message = creatorsResult.message, onRetry = onRetry)
        }
        is ApiResult.Success -> {
            val items = creatorsResult.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!item.creatorAvatar.isNullOrBlank()) {
                                    AsyncImage(
                                        model = item.creatorAvatar,
                                        contentDescription = item.creatorName,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.brandPillBackground),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = TravelPrimary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = item.creatorName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = TravelWarning, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("${item.rating}", style = MaterialTheme.typography.labelSmall)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("•  ${item.copyCount} travelers copied", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${item.durationDays} Days") },
                                    leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text(item.destinationName) },
                                    leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Estimated Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("₹${item.totalEstimatedCost.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TravelPrimary)
                                }

                                Button(
                                    onClick = { onCopyItinerary(item.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy to My Trips", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePostDialog(
    onDismiss: () -> Unit,
    onPost: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share with Travelers", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Subject / Title") },
                    placeholder = { Text("e.g. Best cafe in Udaipur") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Your Travel Tip / Experience") },
                    placeholder = { Text("Share details, best timings, prices...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onPost(title, content)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
            ) {
                Text("Publish Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
