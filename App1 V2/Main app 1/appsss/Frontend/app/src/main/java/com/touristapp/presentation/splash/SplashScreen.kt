package com.touristapp.presentation.splash

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.touristapp.R
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToWalkthrough: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: (Int) -> Unit
) {
    val sessionRepository = ServiceLocator.sessionRepository
    val hasSeenWalkthrough by sessionRepository.hasSeenWalkthroughFlow.collectAsState(initial = false)
    val hasCompletedOnboarding by sessionRepository.hasCompletedOnboardingFlow.collectAsState(initial = false)
    val savedStep by sessionRepository.onboardingStepFlow.collectAsState(initial = 1)
    val isGuest by sessionRepository.isGuestFlow.collectAsState(initial = false)

    LaunchedEffect(hasSeenWalkthrough, hasCompletedOnboarding, isGuest, savedStep) {
        delay(600) // Brief brand splash display
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isRealUserLoggedIn = currentUser != null && !currentUser.isAnonymous

        if (!hasSeenWalkthrough || (!isRealUserLoggedIn && !isGuest)) {
            onNavigateToWalkthrough()
        } else if (isGuest || isRealUserLoggedIn) {
            if (!hasCompletedOnboarding) {
                onNavigateToOnboarding(savedStep)
            } else {
                onNavigateToHome()
            }
        } else {
            onNavigateToWalkthrough()
        }
    }

    // Subtle pulse animation for the mascot logo
    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1A2E),
                        Color(0xFF0F2440),
                        Color(0xFF070E18)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.auralis_logo),
                contentDescription = "Auralis Logo",
                modifier = Modifier
                    .size(100.dp)
                    .scale(logoScale)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "AURALIS",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFBBF24),
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "TRAVEL SMARTER • SAFER • HAPPIER",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.75f),
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            CircularProgressIndicator(
                color = Color(0xFFFBBF24),
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.5.dp
            )
        }
    }
}
