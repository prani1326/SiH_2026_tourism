package com.travellikepro.opsleader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travellikepro.opsleader.ui.theme.StatusAttention
import com.travellikepro.opsleader.ui.theme.StatusCritical
import com.travellikepro.opsleader.ui.theme.StatusEmergency
import com.travellikepro.opsleader.ui.theme.StatusNormal
import com.travellikepro.opsleader.ui.theme.StatusResolved
import com.travellikepro.opsleader.ui.theme.StatusWarning

enum class StatusLevel {
    NORMAL, ATTENTION, WARNING, CRITICAL, EMERGENCY, RESOLVED
}

@Composable
fun StatusBadge(
    status: StatusLevel,
    label: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (status) {
        StatusLevel.NORMAL -> StatusNormal
        StatusLevel.ATTENTION -> StatusAttention
        StatusLevel.WARNING -> StatusWarning
        StatusLevel.CRITICAL -> StatusCritical
        StatusLevel.EMERGENCY -> StatusEmergency
        StatusLevel.RESOLVED -> StatusResolved
    }

    Box(
        modifier = modifier
            .background(color = backgroundColor.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            color = backgroundColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
