package com.travellikepro.opsleader.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.travellikepro.opsleader.navigation.Routes
import com.travellikepro.opsleader.ui.theme.StatusCritical

import androidx.compose.material.icons.automirrored.filled.Assignment

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val hasBadge: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Primary 4-tab Bottom Navigation
    val bottomNavItems = listOf(
        BottomNavItem(Routes.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
        BottomNavItem(Routes.REQUESTS, "Requests", Icons.AutoMirrored.Filled.Assignment),
        BottomNavItem(Routes.VENDORS, "Vendors", Icons.Default.Storefront),
        BottomNavItem(Routes.PROFILE, "Profile", Icons.Default.AccountCircle)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leader", fontWeight = FontWeight.Bold) },
                actions = {
                    FilledTonalButton(
                        onClick = { navController.navigate(Routes.SAFETY_HUB) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StatusCritical.copy(alpha = 0.14f),
                            contentColor = StatusCritical
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Emergency,
                            contentDescription = "Safety Hub",
                            tint = StatusCritical,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SOS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatusCritical
                        )
                    }
                    IconButton(onClick = { navController.navigate(Routes.SUPPORT) }) {
                        Icon(Icons.Default.SupportAgent, contentDescription = "Support & Messaging")
                    }
                    IconButton(onClick = { navController.navigate(Routes.ANALYTICS) }) {
                        Icon(Icons.Default.Analytics, contentDescription = "Analytics & Reports")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route || (item.route == Routes.REQUESTS && currentRoute?.startsWith(Routes.REQUESTS) == true)
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(item.icon, contentDescription = item.title)
                        },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            MainNavigationGraph(
                navController = navController,
                onLogout = onLogout
            )
        }
    }
}

@Composable
fun MainNavigationGraph(
    navController: NavHostController,
    onLogout: () -> Unit = {}
) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        // V4: Operational Dashboard
        composable(Routes.DASHBOARD) {
            com.travellikepro.opsleader.ui.main.dashboard.DashboardScreen(
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToRequests = { navController.navigate(Routes.REQUESTS) },
                onNavigateToSafety = { navController.navigate(Routes.SAFETY_HUB) },
                onNavigateToSupport = { navController.navigate(Routes.SUPPORT) },
                onNavigateToAnalytics = { navController.navigate(Routes.ANALYTICS) },
                onNavigateToVendors = { navController.navigate(Routes.VENDORS) }
            )
        }

        // V1 & V2: Requests & Bookings
        composable(Routes.REQUESTS) {
            com.travellikepro.opsleader.ui.bookings.BookingsScreen(
                onNavigateToTripRequestDetail = { requestId ->
                    navController.navigate("${Routes.REQUESTS}/$requestId")
                }
            )
        }
        composable("${Routes.REQUESTS}/{requestId}") { backStackEntry ->
            val requestId = backStackEntry.arguments?.getString("requestId") ?: ""
            com.travellikepro.opsleader.ui.triprequests.TripRequestDetailScreen(
                requestId = requestId,
                onBack = { navController.popBackStack() }
            )
        }

        // V1: Vendors / Partners Directory
        composable(Routes.VENDORS) {
            com.travellikepro.opsleader.ui.vendors.VendorsScreen()
        }

        // Profile & Settings
        composable(Routes.PROFILE) {
            com.travellikepro.opsleader.ui.profile.ProfileScreen(
                onLogout = onLogout
            )
        }

        // V3: Safety & SOS Hub
        composable(Routes.SAFETY_HUB) {
            com.travellikepro.opsleader.ui.safety.SafetyHubScreen()
        }

        // V2: Support & Messaging
        composable(Routes.SUPPORT) {
            com.travellikepro.opsleader.ui.support.SupportScreen()
        }

        // V4: Tourists Directory & Detail
        composable(Routes.TOURISTS) {
            com.travellikepro.opsleader.ui.tourists.TouristsListScreen(
                onNavigateToTouristDetail = { touristId ->
                    navController.navigate("${Routes.TOURISTS}/$touristId")
                }
            )
        }
        composable("${Routes.TOURISTS}/{touristId}") { backStackEntry ->
            val touristId = backStackEntry.arguments?.getString("touristId") ?: ""
            com.travellikepro.opsleader.ui.tourists.TouristDetailScreen(
                touristId = touristId,
                onBack = { navController.popBackStack() }
            )
        }

        // V4: Analytics & Reports
        composable(Routes.ANALYTICS) {
            com.travellikepro.opsleader.ui.analytics.AnalyticsScreen()
        }
    }
}
