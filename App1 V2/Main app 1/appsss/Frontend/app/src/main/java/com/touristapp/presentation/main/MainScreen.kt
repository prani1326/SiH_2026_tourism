package com.touristapp.presentation.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.touristapp.di.ServiceLocator
import com.touristapp.presentation.explore.ExploreScreen
import com.touristapp.presentation.home.HomeScreen
import com.touristapp.presentation.navigation.Screen
import com.touristapp.presentation.profile.ProfileScreen
import com.touristapp.ui.components.BottomNavBar
import com.touristapp.ui.components.NavRoute
import com.touristapp.ui.theme.TravelPrimary
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    rootNavController: NavController
) {
    var currentRoute by remember { mutableStateOf(NavRoute.Home) }

    val coroutineScope = rememberCoroutineScope()
    var lockedEligibility by remember { mutableStateOf<com.touristapp.data.models.TripEligibilityResponse?>(null) }
    var showLockSheet by remember { mutableStateOf(false) }
    var showKycDialogFromLock by remember { mutableStateOf(false) }

    val handlePlanTripAttempt: (String?) -> Unit = { dest ->
        coroutineScope.launch {
            val res = ServiceLocator.backendApiClient.checkTripEligibility()
            if (res.isSuccess) {
                val eligibility = res.getOrNull()
                if (eligibility != null && eligibility.eligible) {
                    rootNavController.navigate(Screen.AiPlanner.createRoute(dest))
                } else {
                    lockedEligibility = eligibility ?: com.touristapp.data.models.TripEligibilityResponse(
                        eligible = false,
                        profile_complete = false,
                        kyc_verified = false,
                        kyc_status = "NOT_STARTED",
                        missing_requirements = listOf("KYC_VERIFICATION")
                    )
                    showLockSheet = true
                }
            } else {
                // Fallback to local session check
                val kycStatus = ServiceLocator.sessionRepository.kycStatusFlow.firstOrNull() ?: "NOT_STARTED"
                val isKycVerified = kycStatus == "VERIFIED"
                if (isKycVerified) {
                    rootNavController.navigate(Screen.AiPlanner.createRoute(dest))
                } else {
                    lockedEligibility = com.touristapp.data.models.TripEligibilityResponse(
                        eligible = false,
                        profile_complete = false,
                        kyc_verified = false,
                        kyc_status = kycStatus,
                        missing_requirements = listOf("KYC_VERIFICATION")
                    )
                    showLockSheet = true
                }
            }
        }
    }

    if (showLockSheet) {
        com.touristapp.presentation.trips.components.TripPlanningLockBottomSheet(
            eligibility = lockedEligibility,
            onDismiss = { showLockSheet = false },
            onCompleteKycClick = {
                showLockSheet = false
                showKycDialogFromLock = true
            },
            onCompleteProfileClick = {
                showLockSheet = false
                rootNavController.navigate(Screen.ProfileOnboarding.createRoute(1))
            }
        )
    }

    if (showKycDialogFromLock) {
        com.touristapp.presentation.profile.components.KycVerificationDialog(
            onDismiss = { showKycDialogFromLock = false },
            onVerificationSuccess = {
                showKycDialogFromLock = false
                rootNavController.navigate(Screen.AiPlanner.createRoute())
            }
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            when (currentRoute) {
                NavRoute.Home -> {
                    HomeScreen(
                        onSearchClick = { rootNavController.navigate(Screen.Search.route) },
                        onDestinationClick = { id -> rootNavController.navigate(Screen.DestinationDetail.createRoute(id)) },
                        onSosClick = { rootNavController.navigate(Screen.Safety.route) },
                        onPlanTripClick = { handlePlanTripAttempt(null) },
                        onLoginClick = {
                            rootNavController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onToolsClick = { rootNavController.navigate(Screen.TravelTools.route) },
                        onGuideClick = { rootNavController.navigate(Screen.Guide.route) },
                        onBookingsClick = { rootNavController.navigate(Screen.Bookings.route) },
                        onSupportClick = { rootNavController.navigate(Screen.Support.route) },
                        onMapClick = { rootNavController.navigate(Screen.MapScreen.createRoute()) },
                        onTrackTripClick = { rootNavController.navigate(Screen.TrackTrip.createRoute()) },
                        onAiChatClick = { prompt -> rootNavController.navigate(Screen.AiAssistant.createRoute(prompt)) },
                        onSafetyIntelligenceClick = { rootNavController.navigate(Screen.SafetyIntelligence.createRoute()) },
                        onGuardianClick = { rootNavController.navigate(Screen.Guardian.createRoute()) },
                        onScamShieldClick = { rootNavController.navigate(Screen.ScamShield.createRoute()) },
                        onHeritageLensClick = { rootNavController.navigate(Screen.HeritageLens.route) }
                    )
                }
                NavRoute.Explore -> {
                    ExploreScreen(
                        onDestinationClick = { id -> rootNavController.navigate(Screen.DestinationDetail.createRoute(id)) }
                    )
                }
                NavRoute.Trips -> {
                    com.touristapp.presentation.trips.TripsScreen(
                        onTripClick = { id -> rootNavController.navigate(Screen.Itinerary.createRoute(id)) },
                        onPlanNewTripClick = { handlePlanTripAttempt(null) }
                    )
                }
                NavRoute.Community -> {
                    com.touristapp.presentation.community.CommunityScreen(
                        onItineraryClick = { id -> rootNavController.navigate(Screen.Itinerary.createRoute(id)) }
                    )
                }
                NavRoute.Profile -> {
                    ProfileScreen(
                        onLogoutClick = {
                            rootNavController.navigate(Screen.Walkthrough.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        onBookingsClick = { rootNavController.navigate(Screen.Bookings.route) },
                        onToolsClick = { rootNavController.navigate(Screen.TravelTools.route) },
                        onSupportClick = { rootNavController.navigate(Screen.Support.route) },
                        onOpenOnboarding = { rootNavController.navigate(Screen.ProfileOnboarding.createRoute(1)) },
                        onGuardianClick = { rootNavController.navigate(Screen.Guardian.createRoute()) },
                        onSafetyClick = { rootNavController.navigate(Screen.SafetyIntelligence.createRoute()) },
                        onScamShieldClick = { rootNavController.navigate(Screen.ScamShield.createRoute()) }
                    )
                }
            }

            // Floating Circular "✨ AI Chat" Button positioned directly above Profile tab (bottom right)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 10.dp)
                    .size(44.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        rootNavController.navigate(Screen.AiAssistant.createRoute())
                    },
                shape = CircleShape,
                color = TravelPrimary
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "AI Chat",
                        tint = Color(0xFFFFEB3B),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
