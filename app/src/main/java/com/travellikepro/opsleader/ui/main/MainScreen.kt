package com.travellikepro.opsleader.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.travellikepro.opsleader.navigation.Routes

data class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Routes.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
        BottomNavItem(Routes.REQUESTS, "Requests", Icons.Default.BookOnline),
        BottomNavItem(Routes.VENDORS, "Vendors", Icons.Default.Storefront),
        BottomNavItem(Routes.TOURISTS, "Tourists", Icons.Default.Group),
        BottomNavItem(Routes.PROFILE, "Profile", Icons.Default.Person)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ops Leader") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            MainNavigationGraph(navController = navController)
        }
    }
}

@Composable
fun MainNavigationGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            com.travellikepro.opsleader.ui.main.dashboard.DashboardScreen(
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onNavigateToRequests = {
                    navController.navigate(Routes.REQUESTS)
                }
            )
        }
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
        composable(Routes.VENDORS) {
            com.travellikepro.opsleader.ui.vendors.VendorsScreen()
        }
        composable(Routes.TOURISTS) {
            com.travellikepro.opsleader.ui.tourists.TouristDetailScreen(
                touristId = "T-123",
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PROFILE) {
            com.travellikepro.opsleader.ui.profile.ProfileScreen()
        }
    }
}
