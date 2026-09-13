package com.touristapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.touristapp.data.repository.SessionRepository
import kotlinx.coroutines.launch
import com.touristapp.presentation.auth.*
import com.touristapp.presentation.destination.DestinationDetailScreen
import com.touristapp.presentation.explore.ExploreScreen
import com.touristapp.presentation.home.HomeScreen
import com.touristapp.presentation.onboarding.TravelPreferenceOnboarding
import com.touristapp.presentation.profile.ProfileScreen
import com.touristapp.presentation.safety.*
import com.touristapp.presentation.search.SearchScreen
import com.touristapp.presentation.splash.SplashScreen
import com.touristapp.presentation.tripcard.TripCardScreen
import com.touristapp.presentation.trips.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToWalkthrough = {
                    navController.navigate(Screen.Walkthrough.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = { step ->
                    navController.navigate(Screen.ProfileOnboarding.createRoute(step)) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Walkthrough.route) {
            com.touristapp.presentation.walkthrough.WalkthroughScreen(
                onFinishWalkthrough = {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    val isRealUserLoggedIn = currentUser != null && !currentUser.isAnonymous
                    if (isRealUserLoggedIn) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Walkthrough.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Walkthrough.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSignupClick = { navController.navigate(Screen.Signup.route) },
                onForgotPasswordClick = { navController.navigate(Screen.ForgotPassword.route) },
                onPhoneLoginClick = { navController.navigate(Screen.Otp.createRoute("phone")) }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.ProfileOnboarding.createRoute(1)) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onLoginClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ProfileOnboarding.route,
            arguments = listOf(
                navArgument("step") {
                    type = NavType.IntType
                    defaultValue = 1
                }
            )
        ) { backStackEntry ->
            val step = backStackEntry.arguments?.getInt("step") ?: 1
            com.touristapp.presentation.onboarding.ProfileOnboardingScreen(
                initialStep = step,
                onOnboardingComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ProfileOnboarding.route) { inclusive = true }
                    }
                },
                onSkipKycComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ProfileOnboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            val context = LocalContext.current
            val authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = AuthViewModelFactory(context)
            )
            ForgotPasswordScreen(
                onRequestReset = { emailOrPhone ->
                    authViewModel.sendPasswordReset(emailOrPhone)
                    android.widget.Toast.makeText(context, "Password reset email sent if account exists.", android.widget.Toast.LENGTH_LONG).show()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Otp.route,
            arguments = listOf(navArgument("emailOrPhone") { type = NavType.StringType })
        ) { backStackEntry ->
            val emailOrPhone = backStackEntry.arguments?.getString("emailOrPhone") ?: ""
            OtpScreen(
                emailOrPhone = emailOrPhone,
                onSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Onboarding.route) {
            val context = LocalContext.current
            val viewModel: com.touristapp.presentation.onboarding.OnboardingViewModel = 
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.touristapp.presentation.onboarding.OnboardingViewModelFactory(context)
                )

            TravelPreferenceOnboarding(
                onComplete = { preferences ->
                    @Suppress("UNCHECKED_CAST")
                    viewModel.completeOnboarding(
                        destinations = preferences["destinations"] as? List<String> ?: emptyList(),
                        styles = preferences["styles"] as? List<String> ?: emptyList(),
                        interests = preferences["interests"] as? List<String> ?: emptyList(),
                        onSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                },
                onSkip = {
                    viewModel.skipOnboarding(
                        onSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
            )
        }

        // --- Main App ---

        composable(Screen.Home.route) {
            com.touristapp.presentation.main.MainScreen(rootNavController = navController)
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBackClick = { navController.popBackStack() },
                onResultClick = { id -> navController.navigate(Screen.DestinationDetail.createRoute(id)) }
            )
        }

        composable(
            route = Screen.DestinationDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            DestinationDetailScreen(
                destinationId = id,
                onBackClick = { navController.popBackStack() },
                onPlanTripClick = { destName ->
                    navController.navigate(Screen.AiPlanner.createRoute(destName))
                },
                onBookingsClick = {
                    navController.navigate(Screen.Bookings.route)
                }
            )
        }

        composable(
            route = Screen.AiPlanner.route,
            arguments = listOf(
                navArgument("destination") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val prefilled = backStackEntry.arguments?.getString("destination")
                ?.takeIf { it.isNotBlank() && it != "{destination}" }
            AiPlannerScreen(
                initialDestination = prefilled,
                onBackClick = { navController.popBackStack() },
                onPlanGenerated = { tripId ->
                    navController.navigate(Screen.Itinerary.createRoute(tripId)) {
                        popUpTo(Screen.AiPlanner.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Itinerary.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            ItineraryScreen(
                tripId = id,
                onBackClick = { navController.popBackStack() },
                onBookTripClick = { tripId ->
                    navController.navigate(Screen.BookingReview.createRoute(tripId))
                },
                onViewTripCard = { tripId ->
                    navController.navigate(Screen.TripCard.createRoute(tripId))
                },
                onOpenMapClick = { tripId, _ ->
                    navController.navigate(Screen.TrackTrip.createRoute(tripId))
                }
            )
        }

        composable(
            route = Screen.BookingReview.route,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            com.touristapp.presentation.bookings.BookingReviewScreen(
                tripId = tripId,
                onBackClick = { navController.popBackStack() },
                onProceedToPayment = { id, amount ->
                    navController.navigate(Screen.Payment.createRoute(id, amount))
                }
            )
        }

        composable(
            route = Screen.Payment.route,
            arguments = listOf(
                navArgument("tripId") { type = NavType.StringType },
                navArgument("amount") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = "0.0"
                }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val amountStr = backStackEntry.arguments?.getString("amount") ?: "0.0"
            val totalAmount = amountStr.toDoubleOrNull() ?: 18000.0

            com.touristapp.presentation.bookings.PaymentScreen(
                tripId = tripId,
                totalAmount = totalAmount,
                onBackClick = { navController.popBackStack() },
                onPaymentSuccess = { bookingId ->
                    navController.navigate(Screen.BookingConfirmation.createRoute(bookingId)) {
                        popUpTo(Screen.Itinerary.createRoute(tripId)) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.BookingConfirmation.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            com.touristapp.presentation.bookings.BookingConfirmationScreen(
                bookingId = bookingId,
                onBackClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onViewTripClick = { tripId ->
                    navController.navigate(Screen.Itinerary.createRoute(tripId))
                },
                onViewInvoiceClick = { bId ->
                    navController.navigate(Screen.Invoice.createRoute(bId))
                }
            )
        }

        composable(
            route = Screen.Invoice.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            com.touristapp.presentation.bookings.InvoiceScreen(
                bookingId = bookingId,
                onBackClick = { navController.popBackStack() },
                onViewTripClick = { tripId ->
                    navController.navigate(Screen.Itinerary.createRoute(tripId))
                }
            )
        }

        composable(
            route = Screen.TripCard.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            TripCardScreen(
                tripId = id,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.TrueCost.route) {
            TrueCostScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Safety.route) {
            SafetyScreen(
                onBackClick = { navController.popBackStack() },
                onOpenEmergencySos = { navController.navigate(Screen.EmergencySos.createRoute()) },
                onOpenEmergencyContacts = { navController.navigate(Screen.EmergencyContacts.route) },
                onOpenNearbyHelp = { navController.navigate(Screen.NearbyHelp.route) },
                onOpenLostPhoneSetup = { navController.navigate(Screen.LostPhoneSetup.route) },
                onOpenSafetyGroupConfig = { navController.navigate(Screen.SafetyGroupConfig.route) },
                onOpenIncidentReport = { navController.navigate(Screen.IncidentReport.createRoute()) }
            )
        }

        composable(Screen.SafetyGroupConfig.route) {
            SafetyGroupConfigScreen(
                onBackClick = { navController.popBackStack() },
                onSavedSuccessfully = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EmergencySos.route,
            arguments = listOf(
                navArgument("tripId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId")
            EmergencySosScreen(
                tripId = tripId,
                onBackClick = { navController.popBackStack() },
                onSosDispatched = { alertId ->
                    navController.navigate(Screen.ActiveSos.createRoute(alertId)) {
                        popUpTo(Screen.Safety.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.ActiveSos.route,
            arguments = listOf(
                navArgument("alertId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString("alertId") ?: ""
            ActiveSosScreen(
                alertId = alertId,
                onBackClick = { navController.popBackStack() },
                onEmergencyResolved = {
                    navController.popBackStack(Screen.Safety.route, inclusive = false)
                },
                onOpenNearbyHelp = {
                    navController.navigate(Screen.NearbyHelp.route)
                }
            )
        }

        composable(Screen.EmergencyContacts.route) {
            EmergencyContactsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.NearbyHelp.route) {
            NearbyHelpScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToMap = { _, _, _ ->
                    navController.navigate(Screen.MapScreen.createRoute(null))
                }
            )
        }

        composable(Screen.LostPhoneSetup.route) {
            LostPhoneSetupScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.IncidentReport.route,
            arguments = listOf(
                navArgument("tripId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId")
            IncidentReportScreen(
                tripId = tripId,
                onBackClick = { navController.popBackStack() },
                onReportSubmitted = { navController.popBackStack() }
            )
        }

        composable(Screen.Bookings.route) {
            com.touristapp.presentation.bookings.BookingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Support.route) {
            com.touristapp.presentation.support.SupportScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.TravelTools.route) {
            com.touristapp.presentation.tools.TravelToolsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.MapScreen.route,
            arguments = listOf(
                navArgument("destinationId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val destinationId = backStackEntry.arguments?.getString("destinationId")
                ?.takeIf { it.isNotBlank() && it != "{destinationId}" }
            com.touristapp.presentation.map.MapScreen(
                destinationId = destinationId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.TrackTrip.route,
            arguments = listOf(
                navArgument("tripId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId")
                ?.takeIf { it.isNotBlank() && it != "{tripId}" }
            com.touristapp.presentation.track.TrackTripScreen(
                tripId = tripId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AiAssistant.route,
            arguments = listOf(
                navArgument("initialPrompt") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val initialPrompt = backStackEntry.arguments?.getString("initialPrompt")
                ?.takeIf { it.isNotBlank() && it != "{initialPrompt}" }
            com.touristapp.presentation.chat.AiAssistantScreen(
                initialPrompt = initialPrompt,
                onBackClick = { navController.popBackStack() },
                onNavigateToTrips = { navController.navigate(Screen.Home.route) },
                onNavigateToBookings = { navController.navigate(Screen.Bookings.route) },
                onNavigateToSafety = { navController.navigate(Screen.Safety.route) },
                onNavigateToEmergencySos = { navController.navigate(Screen.EmergencySos.createRoute()) },
                onNavigateToMap = { destId -> navController.navigate(Screen.MapScreen.createRoute(destId)) },
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                onNavigateToTripDetail = { tripId -> navController.navigate(Screen.Itinerary.createRoute(tripId)) }
            )
        }

        // ==========================================
        // Guide Feature System
        // ==========================================
        composable(route = Screen.Guide.route) {
            com.touristapp.presentation.guide.GuideDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onCameraTranslationClick = { navController.navigate(Screen.GuideCameraTranslate.route) },
                onPlaceDetectionClick = { navController.navigate(Screen.GuidePlaceDetection.route) },
                onVoiceTranslationClick = { navController.navigate(Screen.GuideVoiceTranslate.route) },
                onTravelGuideClick = { navController.navigate(Screen.GuideTravelInfo.route) },
                onSavedItemsClick = { navController.navigate(Screen.GuideSaved.route) },
                onHeritageLensClick = { navController.navigate(Screen.HeritageLens.route) }
            )
        }

        composable(route = Screen.GuideCameraTranslate.route) {
            val guideViewModel: com.touristapp.presentation.guide.GuideViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            com.touristapp.presentation.guide.CameraTranslationScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = guideViewModel
            )
        }

        composable(route = Screen.GuidePlaceDetection.route) {
            val guideViewModel: com.touristapp.presentation.guide.GuideViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            com.touristapp.presentation.guide.PlaceDetectionScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToMap = { destId -> navController.navigate(Screen.MapScreen.createRoute(destId)) },
                viewModel = guideViewModel
            )
        }

        composable(route = Screen.GuideVoiceTranslate.route) {
            val guideViewModel: com.touristapp.presentation.guide.GuideViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            com.touristapp.presentation.guide.VoiceTranslationScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = guideViewModel
            )
        }

        composable(route = Screen.GuideTravelInfo.route) {
            val guideViewModel: com.touristapp.presentation.guide.GuideViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            com.touristapp.presentation.guide.TravelGuideScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = guideViewModel
            )
        }

        composable(route = Screen.GuideSaved.route) {
            val guideViewModel: com.touristapp.presentation.guide.GuideViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            com.touristapp.presentation.guide.SavedTranslationsScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = guideViewModel
            )
        }

        // ==========================================
        // SIH AI Tourist Operating System Screens
        // ==========================================
        composable(
            route = Screen.SafetyIntelligence.route,
            arguments = listOf(navArgument("destinationId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val dest = backStackEntry.arguments?.getString("destinationId") ?: "Jaipur"
            SafetyIntelligenceScreen(
                onBackClick = { navController.popBackStack() },
                onSosClick = {
                    navController.navigate(Screen.EmergencySos.createRoute())
                },
                destination = dest
            )
        }

        composable(
            route = Screen.Guardian.route,
            arguments = listOf(navArgument("tripId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            GuardianScreen(
                onBackClick = { navController.popBackStack() },
                onEmergencyClick = {
                    navController.navigate(Screen.EmergencySos.createRoute())
                }
            )
        }

        composable(
            route = Screen.ScamShield.route,
            arguments = listOf(navArgument("destinationId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val dest = backStackEntry.arguments?.getString("destinationId") ?: "Jaipur"
            ScamShieldScreen(
                onBackClick = { navController.popBackStack() },
                destination = dest
            )
        }

        composable(route = Screen.HeritageLens.route) {
            com.touristapp.presentation.guide.HeritageLensScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}