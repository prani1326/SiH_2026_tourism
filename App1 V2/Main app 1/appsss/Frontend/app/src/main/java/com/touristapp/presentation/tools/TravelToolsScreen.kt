package com.touristapp.presentation.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.touristapp.data.remote.ApiResult
import com.touristapp.data.remote.model.SimPlanDto
import com.touristapp.data.remote.model.VisaDocumentDto
import com.touristapp.ui.components.EmptyState
import com.touristapp.ui.components.ErrorState
import com.touristapp.ui.components.SkeletonLoader
import com.touristapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelToolsScreen(
    onBackClick: () -> Unit,
    viewModel: ToolsViewModel = viewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("eSIM & Data", "Visa Vault", "Menu Scan", "Translate", "Insurance", "Packing List", "Transport Brain")

    val simResult by viewModel.simPlans.collectAsState()
    val visaResult by viewModel.visaDocs.collectAsState()
    val translatedResult by viewModel.translatedResult.collectAsState()
    val menuScanResult by viewModel.menuScanResult.collectAsState()
    val insuranceResult by viewModel.insurancePolicies.collectAsState()
    val preparationResult by viewModel.preparationChecklist.collectAsState()
    val transportResult by viewModel.transportComparison.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Traveler Utilities", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
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
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = TravelPrimary,
                edgePadding = 16.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> SimEsimTab(simResult = simResult, onBuy = { viewModel.purchaseSim(it) }, onRetry = { viewModel.loadSimPlans() })
                1 -> VisaVaultTab(visaResult = visaResult, onRetry = { viewModel.loadVisaDocs() })
                2 -> MenuScanTab(scanResult = menuScanResult, onScan = { text, diet -> viewModel.scanMenu(text, diet) })
                3 -> TranslationTab(translatedText = translatedResult, onTranslate = { text, lang -> viewModel.translate(text, lang) })
                4 -> InsuranceTab(insuranceResult = insuranceResult, onBuy = { viewModel.purchaseInsurance(it) }, onRetry = { viewModel.loadInsurancePolicies() })
                5 -> PreparationTab(prepResult = preparationResult, onSelectDest = { viewModel.loadPreparationChecklist(it) })
                6 -> TransportBrainTab(transportResult = transportResult, onSelectCity = { viewModel.loadTransportComparison(it) })
            }
        }
    }
}

@Composable
private fun SimEsimTab(
    simResult: ApiResult<List<SimPlanDto>>,
    onBuy: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (simResult) {
        is ApiResult.Loading -> {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(120.dp)) }
            }
        }
        is ApiResult.Exception -> {
            ErrorState(message = (simResult as ApiResult.Exception).e.message ?: "Could not load SIM plans", onRetry = onRetry)
        }
        is ApiResult.Error -> {
            ErrorState(message = (simResult as ApiResult.Error).message, onRetry = onRetry)
        }
        is ApiResult.Success -> {
            val plans = simResult.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(plans) { plan ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.SimCard, contentDescription = null, tint = TravelPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(plan.provider, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.brandPillBackground
                                ) {
                                    Text(
                                        if (plan.isEsim) "Instant eSIM" else "Physical SIM",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(plan.dataAllowance, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                            Text("Validity: ${plan.validityDays} Days • Zero Roaming Fees", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("₹${plan.priceInr.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TravelPrimary)

                                Button(
                                    onClick = { onBuy(plan.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                                ) {
                                    Text("Activate eSIM", fontWeight = FontWeight.Bold)
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
private fun VisaVaultTab(
    visaResult: ApiResult<List<VisaDocumentDto>>,
    onRetry: () -> Unit
) {
    when (visaResult) {
        is ApiResult.Loading -> {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(100.dp)) }
            }
        }
        is ApiResult.Exception -> {
            ErrorState(message = (visaResult as ApiResult.Exception).e.message ?: "Failed to load Visa Vault", onRetry = onRetry)
        }
        is ApiResult.Error -> {
            ErrorState(message = (visaResult as ApiResult.Error).message, onRetry = onRetry)
        }
        is ApiResult.Success -> {
            val docs = visaResult.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        "Your Encrypted Document Vault",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Stored securely for offline border access & emergency verification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                items(docs) { doc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = TravelSuccess, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.documentType, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Country: ${doc.country} • Status: ${doc.status}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Filled.FileDownload, contentDescription = "Download", tint = TravelPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuScanTab(
    scanResult: com.touristapp.data.remote.model.MenuScanResponseDto?,
    onScan: (menuText: String, diet: String) -> Unit
) {
    var menuText by remember { mutableStateOf("Paneer Butter Masala\nChicken Biryani\nDal Tadka Pure Ghee\nAloo Gobhi") }
    var selectedDiet by remember { mutableStateOf("Vegetarian") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("AI Menu & Dietary Suitability Analyzer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Scan or paste menu items to verify Pure Veg, Jain, or Halal compliance.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        item {
            OutlinedTextField(
                value = menuText,
                onValueChange = { menuText = it },
                label = { Text("Menu Items (one per line)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Vegetarian", "Jain", "Halal").forEach { diet ->
                    FilterChip(
                        selected = selectedDiet == diet,
                        onClick = { selectedDiet = diet },
                        label = { Text(diet) }
                    )
                }
            }
        }

        item {
            Button(
                onClick = { onScan(menuText, selectedDiet) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
            ) {
                Icon(Icons.Filled.DocumentScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze Menu for $selectedDiet", fontWeight = FontWeight.Bold)
            }
        }

        scanResult?.let { res ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.brandPillBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Suitability: ${res.suitableItemsCount}/${res.totalItemsAnalyzed} Items Safe",
                            fontWeight = FontWeight.Bold,
                            color = TravelDarkPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        res.items.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (item.isSuitable) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (item.isSuitable) TravelSuccess else TravelEmergency,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${item.itemName}: ${item.reason}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationTab(
    translatedText: String?,
    onTranslate: (text: String, targetLang: String) -> Unit
) {
    var inputText by remember { mutableStateOf("Where is the nearest taxi stand?") }
    var targetLang by remember { mutableStateOf("hi") }

    val languages = listOf("hi" to "Hindi", "es" to "Spanish", "fr" to "French", "de" to "German", "ja" to "Japanese")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Instant Travel Translator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Translate essential phrases for local taxi, markets, and emergency communication.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }

        item {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Text to translate") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            Text("Target Language:", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.take(3).forEach { (code, name) ->
                    FilterChip(
                        selected = targetLang == code,
                        onClick = { targetLang = code },
                        label = { Text(name) }
                    )
                }
            }
        }

        item {
            Button(
                onClick = { onTranslate(inputText, targetLang) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
            ) {
                Icon(Icons.Filled.Translate, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Translate Text", fontWeight = FontWeight.Bold)
            }
        }

        translatedText?.let { result ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.brandPillBackground)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Translation Result:", fontWeight = FontWeight.Bold, color = TravelPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(result, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsuranceTab(
    insuranceResult: ApiResult<List<com.touristapp.data.remote.model.InsurancePolicyDto>>,
    onBuy: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (insuranceResult) {
        is ApiResult.Loading -> {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(120.dp)) }
            }
        }
        is ApiResult.Exception -> {
            ErrorState(message = (insuranceResult as ApiResult.Exception).e.message ?: "Failed to load insurance policies", onRetry = onRetry)
        }
        is ApiResult.Error -> {
            ErrorState(message = (insuranceResult as ApiResult.Error).message, onRetry = onRetry)
        }
        is ApiResult.Success -> {
            val policies = insuranceResult.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("Travel Insurance & Medical Protection", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Instant cashless claim coverage with certified insurance partners.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                items(policies) { policy ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(policy.provider, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text("₹${policy.premiumInr.toInt()}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(policy.coverageDetails, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Max Claim Coverage: ₹${policy.coverageAmountInr.toInt()}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium, color = TravelPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onBuy(policy.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Instant Purchase & Issue Policy", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreparationTab(
    prepResult: ApiResult<com.touristapp.data.remote.model.PreparationChecklistDto>,
    onSelectDest: (String) -> Unit
) {
    val destinations = listOf("Jaipur", "Agra", "Goa", "Delhi", "Kerala")
    var selectedDest by remember { mutableStateOf("Jaipur") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            destinations.forEach { dest ->
                FilterChip(
                    selected = selectedDest == dest,
                    onClick = {
                        selectedDest = dest
                        onSelectDest(dest)
                    },
                    label = { Text(dest) }
                )
            }
        }

        when (prepResult) {
            is ApiResult.Loading -> {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(100.dp)) }
                }
            }
            is ApiResult.Exception -> {
                ErrorState(message = (prepResult as ApiResult.Exception).e.message ?: "Failed to load checklist", onRetry = { onSelectDest(selectedDest) })
            }
            is ApiResult.Error -> {
                ErrorState(message = (prepResult as ApiResult.Error).message, onRetry = { onSelectDest(selectedDest) })
            }
            is ApiResult.Success -> {
                val data = prepResult.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Pre-Trip Preparation Checklist: ${data.destination}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    item {
                        Text("🎒 Smart Weather Packing List", fontWeight = FontWeight.Bold, color = TravelPrimary)
                    }
                    items(data.weatherPackingList) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(item.item, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(item.reason, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🏛️ Cultural Etiquette Guidelines", fontWeight = FontWeight.Bold, color = TravelPrimary)
                    }
                    items(data.culturalPreparation) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(item.requirement, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(item.guideline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("💊 Travel Medical Kit", fontWeight = FontWeight.Bold, color = TravelPrimary)
                    }
                    items(data.travelMedicalKit) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(item.item, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(item.purpose, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
private fun TransportBrainTab(
    transportResult: ApiResult<com.touristapp.data.remote.model.TransportComparisonResponseDto>,
    onSelectCity: (String) -> Unit
) {
    val cities = listOf("Agra", "Jaipur", "Goa", "Delhi")
    var selectedCity by remember { mutableStateOf("Agra") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cities.forEach { city ->
                FilterChip(
                    selected = selectedCity == city,
                    onClick = {
                        selectedCity = city
                        onSelectCity(city)
                    },
                    label = { Text(city) }
                )
            }
        }

        when (transportResult) {
            is ApiResult.Loading -> {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(120.dp)) }
                }
            }
            is ApiResult.Exception -> {
                ErrorState(message = (transportResult as ApiResult.Exception).e.message ?: "Failed to load transport options", onRetry = { onSelectCity(selectedCity) })
            }
            is ApiResult.Error -> {
                ErrorState(message = (transportResult as ApiResult.Error).message, onRetry = { onSelectCity(selectedCity) })
            }
            is ApiResult.Success -> {
                val data = transportResult.data
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(data.route, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Real-time fare comparisons, transit speed, and safety ratings.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }

                    items(data.options) { opt ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(opt.mode, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(opt.badge, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("⏱ ${opt.estimatedTimeMins} mins", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text("₹${opt.estimatedCostInr.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TravelPrimary)
                                    Text("⭐ ${opt.safetyRating}/5.0", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("💡 ${opt.tip}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    data.cityPassRecommendation?.let { pass ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.brandPillBackground)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("🎟️ ${pass.name}", fontWeight = FontWeight.Bold, color = TravelPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(pass.benefits, style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("1-Day Pass: ₹${pass.price1Day.toInt()}  •  3-Day Pass: ₹${pass.price3Day.toInt()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

