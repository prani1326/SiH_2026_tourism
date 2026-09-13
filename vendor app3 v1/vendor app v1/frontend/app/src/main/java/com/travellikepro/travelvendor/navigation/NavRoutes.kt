package com.travellikepro.travelvendor.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Login : Screen("login", "Login")
    object SignUp : Screen("signup", "Sign Up")
    object KycCheck : Screen("kyc_check", "Verifying KYC")
    object Kyc : Screen("kyc", "KYC Verification")
    
    // Bottom Nav Tabs
    object Home : Screen("home", "Home", Icons.Outlined.Home)
    object Bookings : Screen("bookings", "Bookings", Icons.Outlined.ConfirmationNumber)
    object Listings : Screen("listings", "Listings", Icons.Outlined.Explore)
    object Wallet : Screen("wallet", "Wallet", Icons.Outlined.AccountBalanceWallet)
    object Profile : Screen("profile", "Profile", Icons.Outlined.Person)

    // Detail screens
    object BookingDetail : Screen("booking_detail/{bookingId}", "Booking Details") {
        fun createRoute(bookingId: Int) = "booking_detail/$bookingId"
    }

    object ListingDetail : Screen("listing_detail/{listingId}", "Listing Details") {
        fun createRoute(listingId: Int) = "listing_detail/$listingId"
    }

    object CreateEditListing : Screen("create_edit_listing?listingId={listingId}", "Create Listing") {
        fun createRoute(listingId: Int? = null) = if (listingId != null) "create_edit_listing?listingId=$listingId" else "create_edit_listing"
    }

    object Notifications : Screen("notifications", "Notifications")
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Bookings,
    Screen.Listings,
    Screen.Wallet,
    Screen.Profile
)
