package com.travellikepro.travelvendor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travellikepro.travelvendor.ui.theme.*

@Composable
fun StatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "approved", "accepted", "completed", "active" -> Triple(
            StatusApprovedBg,
            StatusApprovedText,
            status.uppercase()
        )
        "pending", "upcoming" -> Triple(
            StatusPendingBg,
            StatusPendingText,
            status.uppercase()
        )
        "rejected", "inactive" -> Triple(
            StatusRejectedBg,
            StatusRejectedText,
            status.uppercase()
        )
        else -> Triple(
            StatusBlueBg,
            StatusBlueText,
            status.uppercase()
        )
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
