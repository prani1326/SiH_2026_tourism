package com.touristapp.presentation.walkthrough

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.R
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun WalkthroughScreen(
    onFinishWalkthrough: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val sessionRepository = ServiceLocator.sessionRepository

    val handleFinish = {
        coroutineScope.launch {
            sessionRepository.saveWalkthroughSeen(true)
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            var uid = auth.currentUser?.uid
            if (uid == null) {
                try {
                    uid = auth.signInAnonymously().await().user?.uid
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (uid != null) {
                try {
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection(com.touristapp.data.firebase.FirestoreCollections.USERS)
                        .document(uid)
                        .set(mapOf("has_seen_walkthrough" to true), com.google.firebase.firestore.SetOptions.merge())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onFinishWalkthrough()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // -------------------------------------------------------------
        // HORIZONTAL PAGER: 3 WELCOME SCREENS
        // -------------------------------------------------------------
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ScreenOneWelcome()
                1 -> ScreenTwoPlan()
                2 -> ScreenThreeSafety()
            }
        }

        // -------------------------------------------------------------
        // BOTTOM NAVIGATION (Pager Dots & Pill Button)
        // -------------------------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 3 Circular Pager Indicator Dots (Pixel-perfect match to mockup)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                repeat(3) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 8.dp else 6.5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            // Pill Action Button
            if (pagerState.currentPage < 2) {
                // Screens 1 & 2: White Pill Button with "Next ->"
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0F172A)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Screen 3: Deep Blue Pill Button with "Start Exploring ->"
                Button(
                    onClick = { handleFinish() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A3D78),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Start Exploring",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Start Exploring",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ===========================================================================
// TOP BRANDING COMPONENT (Flows seamlessly inside each screen)
// ===========================================================================
@Composable
private fun TopBranding() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.auralis_logo),
            contentDescription = "Auralis Mascot Logo",
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "AURALIS",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFBBF24),
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "TRAVEL SMARTER • SAFER • HAPPIER",
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.85f),
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ===========================================================================
// SCREEN 1: Welcome Explorer!
// ===========================================================================
@Composable
private fun ScreenOneWelcome() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Mountain lake traveler background (Clean photo)
        Image(
            painter = painterResource(id = R.drawable.welcome_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient & Vignette Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDD081526),
                            Color(0x990E223D),
                            Color(0x330E223D),
                            Color(0x770E223D),
                            Color(0xDD081526),
                            Color(0xF5030B14)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 116.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBranding()

            Spacer(modifier = Modifier.height(18.dp))

            // Heading: Welcome Explorer!
            Text(
                text = "Welcome",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Cursive,
                color = Color.White,
                lineHeight = 40.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Explorer!",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Cursive,
                color = Color(0xFFFBBF24),
                lineHeight = 48.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Discover amazing places,\nplan smarter trips and travel\nwith confidence.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Lower right: Good Trips Brighter You ♥
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Good Trips Brighter You ♥",
                    fontSize = 14.5.sp,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

// ===========================================================================
// SCREEN 2: Plan Better, Explore More
// ===========================================================================
@Composable
private fun ScreenTwoPlan() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Positano Amalfi coast background (Clean photo)
        Image(
            painter = painterResource(id = R.drawable.walkthrough_bg_2),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDD081526),
                            Color(0x990E223D),
                            Color(0x330E223D),
                            Color(0x88081526),
                            Color(0xF5030B14)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 116.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBranding()

            Spacer(modifier = Modifier.height(14.dp))

            // Heading: Plan Better, Explore More
            Text(
                text = "Plan Better,",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Cursive,
                color = Color.White,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Explore More",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Cursive,
                color = Color(0xFFFBBF24),
                lineHeight = 40.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Get personalized itineraries, AI travel tips, hotels, food,\ntransport and local insights — all in one place.",
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Clean White Rounded Feature Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WhiteFeatureCard(
                    customIcon = {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .border(1.5.dp, Color.White, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AI",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    },
                    iconBg = Color(0xFF2563EB),
                    title = "AI Trip Planner",
                    subtitle = "Personalized for you"
                )
                WhiteFeatureCard(
                    icon = Icons.Filled.Hotel,
                    iconBg = Color(0xFF0284C7),
                    title = "Best Hotels",
                    subtitle = "Trusted & Verified"
                )
                WhiteFeatureCard(
                    icon = Icons.Filled.Restaurant,
                    iconBg = Color(0xFFEA580C),
                    title = "Local Food",
                    subtitle = "Taste the Real Culture"
                )
                WhiteFeatureCard(
                    icon = Icons.Filled.LocationOn,
                    iconBg = Color(0xFF16A34A),
                    title = "Find Nearby",
                    subtitle = "Attractions, Transport & More"
                )
            }
        }
    }
}

// ===========================================================================
// SCREEN 3: Travel Safe, Always
// ===========================================================================
@Composable
private fun ScreenThreeSafety() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Traveler woman mountain sunset background (Clean photo)
        Image(
            painter = painterResource(id = R.drawable.walkthrough_bg_3),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDD081526),
                            Color(0x990E223D),
                            Color(0x330E223D),
                            Color(0x88081526),
                            Color(0xF5030B14)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 116.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBranding()

            Spacer(modifier = Modifier.height(14.dp))

            // Heading: Travel Safe, Always
            Text(
                text = "Travel Safe,",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Cursive,
                color = Color.White,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Always",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Cursive,
                color = Color(0xFFFBBF24),
                lineHeight = 40.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Real-time safety alerts, safe routes, emergency help\nand trusted local information — because your safety matters.",
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Clean White Rounded Feature Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WhiteFeatureCard(
                    icon = Icons.Filled.Shield,
                    iconBg = Color(0xFF16A34A),
                    title = "Safety Score",
                    subtitle = "& Risk Alerts"
                )
                WhiteFeatureCard(
                    customIcon = {
                        Text(
                            text = "SOS",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Black
                        )
                    },
                    iconBg = Color(0xFFDC2626),
                    title = "SOS",
                    subtitle = "Emergency Help"
                )
                WhiteFeatureCard(
                    icon = Icons.Filled.Map,
                    iconBg = Color(0xFF0284C7),
                    title = "Safe Routes",
                    subtitle = "with AI Maps"
                )
                WhiteFeatureCard(
                    icon = Icons.Filled.Groups,
                    iconBg = Color(0xFF9333EA),
                    title = "Stay Connected",
                    subtitle = "with Loved Ones"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Script Motto (Bottom Right): Explore with Confidence ♥
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Explore with Confidence ♥",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

// ===========================================================================
// REUSABLE WHITE FEATURE CARD (Matches Mockup)
// ===========================================================================
@Composable
private fun WhiteFeatureCard(
    icon: ImageVector? = null,
    customIcon: (@Composable () -> Unit)? = null,
    iconBg: Color,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.96f),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                if (customIcon != null) {
                    customIcon()
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
