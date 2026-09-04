package com.travellikepro.opsleader.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.travellikepro.opsleader.ui.auth.pending.PendingApprovalScreen
import com.travellikepro.opsleader.ui.auth.signup.SignUpScreen
import com.travellikepro.opsleader.ui.login.LoginScreen
import com.travellikepro.opsleader.ui.main.MainScreen

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val PENDING_APPROVAL = "pending_approval"
    const val MAIN = "main"

    // Sub-routes for the shell (5 primary nav destinations)
    const val DASHBOARD = "dashboard"
    const val REQUESTS = "requests"
    const val VENDORS = "vendors"
    const val TOURISTS = "tourists"
    const val PROFILE = "profile"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Routes.SIGNUP)
                }
            )
        }

        composable(Routes.SIGNUP) {
            SignUpScreen(
                onSignupSuccess = {
                    navController.navigate(Routes.PENDING_APPROVAL) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.PENDING_APPROVAL) {
            PendingApprovalScreen(
                onBackToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.PENDING_APPROVAL) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen()
        }
    }
}
