package com.touristapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.ui.theme.TravelPrimary
import com.touristapp.ui.theme.isDarkMode

/**
 * 5 primary navigation tabs for Tourist Traveler App.
 * Each tab supports both active (filled) and inactive (outlined) icon states.
 */
enum class NavRoute(
    val title: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    Home("Home", Icons.Filled.Home, Icons.Outlined.Home),
    Explore("Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    Trips("Trips", Icons.Filled.Luggage, Icons.Outlined.Luggage),
    Community("Community", Icons.Filled.Forum, Icons.Outlined.Forum),
    Profile("Profile", Icons.Filled.Person, Icons.Outlined.Person);

    // Backward-compatibility property for legacy calls
    val icon: ImageVector get() = activeIcon
}

/**
 * Clean, modern, premium and realistic travel-app bottom navigation bar.
 *
 * Design characteristics:
 * - Clean white background (light) / dark slate (dark) with subtle elevation & border
 * - Softly rounded top corners (20.dp)
 * - Minimal, professional, human-designed aesthetic
 * - Professional travel-app blue primary accent
 * - Dark blue-gray for inactive items
 * - Active tab with light blue pill background, bold title, and subtle bottom indicator
 * - Consistent 22.dp icon sizes and touch-friendly targets
 * - Smooth, subtle transitions and ripple feedback
 * - Trips notification dot indicator for pending/upcoming trips
 * - Respects safe-area navigation bar insets
 */
@Composable
fun BottomNavBar(
    currentRoute: NavRoute,
    onNavigate: (NavRoute) -> Unit,
    modifier: Modifier = Modifier,
    hasUpcomingTrips: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.isDarkMode

    // Professional travel palette
    val navBackgroundColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val topBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
    val activeBlue = TravelPrimary
    val activePillColor = if (isDark) TravelPrimary.copy(alpha = 0.22f) else Color(0xFFE8F0FE)
    val inactiveColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = navBackgroundColor,
        shadowElevation = 8.dp,
        border = BorderStroke(0.75.dp, topBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavRoute.values().forEach { route ->
                    val isSelected = currentRoute == route

                    // Smooth color animations
                    val animPillBg by animateColorAsState(
                        targetValue = if (isSelected) activePillColor else Color.Transparent,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "pillBgColor"
                    )

                    val animIconColor by animateColorAsState(
                        targetValue = if (isSelected) activeBlue else inactiveColor,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "iconColor"
                    )

                    val animTextColor by animateColorAsState(
                        targetValue = if (isSelected) activeBlue else inactiveColor,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "textColor"
                    )

                    val animIndicatorWidth by animateDpAsState(
                        targetValue = if (isSelected) 14.dp else 0.dp,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "indicatorWidth"
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(bounded = false, radius = 26.dp),
                                onClick = { onNavigate(route) }
                            )
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Icon with pill highlight and optional notification badge
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(animPillBg)
                                .padding(horizontal = 14.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) route.activeIcon else route.inactiveIcon,
                                contentDescription = route.title,
                                tint = animIconColor,
                                modifier = Modifier.size(22.dp)
                            )

                            // Subtle notification dot for Trips
                            if (route == NavRoute.Trips && hasUpcomingTrips) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 3.dp, y = (-2).dp)
                                        .background(activeBlue, CircleShape)
                                        .border(1.2.dp, navBackgroundColor, CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Tab title label
                        Text(
                            text = route.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 13.sp,
                                letterSpacing = 0.1.sp
                            ),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = animTextColor,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Active indicator line/dot
                        Box(
                            modifier = Modifier
                                .height(2.5.dp)
                                .width(animIndicatorWidth)
                                .background(
                                    color = if (isSelected) activeBlue else Color.Transparent,
                                    shape = RoundedCornerShape(1.5.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}
