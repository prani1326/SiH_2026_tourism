package com.travellikepro.travelvendor.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.travellikepro.travelvendor.ui.bookings.BookingDetailScreen
import com.travellikepro.travelvendor.ui.bookings.BookingsScreen
import com.travellikepro.travelvendor.ui.home.HomeScreen
import com.travellikepro.travelvendor.ui.kyc.KycScreen
import com.travellikepro.travelvendor.ui.listings.CreateEditListingScreen
import com.travellikepro.travelvendor.ui.listings.ListingDetailScreen
import com.travellikepro.travelvendor.ui.listings.ListingsScreen
import com.travellikepro.travelvendor.ui.login.LoginScreen
import com.travellikepro.travelvendor.ui.signup.SignUpScreen
import com.travellikepro.travelvendor.ui.kyc.KycCheckScreen
import com.travellikepro.travelvendor.ui.notifications.NotificationsScreen
import com.travellikepro.travelvendor.ui.profile.ProfileScreen
import com.travellikepro.travelvendor.ui.wallet.WalletScreen
import com.travellikepro.travelvendor.utils.TokenManager

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val initialRoute = remember {
        if (tokenManager.getToken() != null) Screen.KycCheck.route else Screen.Login.route
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show Bottom Navigation only on main tab screens
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Bookings.route,
        Screen.Listings.route,
        Screen.Wallet.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(imageVector = it, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = initialRoute,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            // Login Screen
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.KycCheck.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp.route)
                    }
                )
            }
            
            // Sign Up Screen
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onSignUpSuccess = {
                        navController.navigate(Screen.KycCheck.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            
            // Kyc Check Screen
            composable(Screen.KycCheck.route) {
                KycCheckScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.KycCheck.route) { inclusive = true }
                        }
                    },
                    onNavigateToKyc = {
                        navController.navigate(Screen.Kyc.route) {
                            popUpTo(Screen.KycCheck.route) { inclusive = true }
                        }
                    },
                    onLogout = {
                        tokenManager.clearToken()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // KYC Screen Gate
            composable(Screen.Kyc.route) {
                KycScreen(
                    onKycApprovedContinue = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Kyc.route) { inclusive = true }
                        }
                    },
                    onLogout = {
                        tokenManager.clearToken()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Home Tab
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToBookings = { navController.navigate(Screen.Bookings.route) },
                    onNavigateToListings = { navController.navigate(Screen.Listings.route) },
                    onNavigateToWallet = { navController.navigate(Screen.Wallet.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToCreateListing = { navController.navigate(Screen.CreateEditListing.createRoute()) }
                )
            }

            // Bookings Tab
            composable(Screen.Bookings.route) {
                BookingsScreen(
                    onNavigateToDetail = { bookingId ->
                        navController.navigate(Screen.BookingDetail.createRoute(bookingId))
                    }
                )
            }

            // Booking Detail
            composable(
                route = Screen.BookingDetail.route,
                arguments = listOf(navArgument("bookingId") { type = NavType.IntType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getInt("bookingId") ?: 0
                BookingDetailScreen(
                    bookingId = bookingId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Listings Tab
            composable(Screen.Listings.route) {
                ListingsScreen(
                    onNavigateToDetail = { listingId ->
                        navController.navigate(Screen.ListingDetail.createRoute(listingId))
                    },
                    onNavigateToCreate = {
                        navController.navigate(Screen.CreateEditListing.createRoute())
                    }
                )
            }

            // Listing Detail
            composable(
                route = Screen.ListingDetail.route,
                arguments = listOf(navArgument("listingId") { type = NavType.IntType })
            ) { backStackEntry ->
                val listingId = backStackEntry.arguments?.getInt("listingId") ?: 0
                ListingDetailScreen(
                    listingId = listingId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(Screen.CreateEditListing.createRoute(id))
                    }
                )
            }

            // Create / Edit Listing Form
            composable(
                route = Screen.CreateEditListing.route,
                arguments = listOf(navArgument("listingId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val listingIdStr = backStackEntry.arguments?.getString("listingId")
                val listingId = listingIdStr?.toIntOrNull()
                CreateEditListingScreen(
                    listingId = listingId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Wallet Tab
            composable(Screen.Wallet.route) {
                WalletScreen()
            }

            // Notifications
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Profile Tab
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogoutSuccess = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}