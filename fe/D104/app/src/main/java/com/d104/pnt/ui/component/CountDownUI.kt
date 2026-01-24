package com.d104.pnt.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun ContDownUI(
    remainingSeconds: Int,
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)

    val isWarning = remainingSeconds in 4..10
    val isDanger = remainingSeconds in 0..3

    // 실제 진동 객체
    val haptic = LocalHapticFeedback.current

    // 마지막 3초에 실제 기기 진동
    LaunchedEffect(remainingSeconds) {
        if (isDanger) {
            haptic.performHapticFeedback(HapticFeedbackType.Reject)
        }
    }

    val textColor by animateColorAsState(
        targetValue = when {
            isDanger -> Color(0xFFFF3B30)
            isWarning -> Color(0xFFFFCC00)
            else -> Color(0xFF9EE7FF)
        },
        label = "TimerColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDanger) 1.15f else 1f,
        label = "TimerScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isWarning || isDanger) 0.85f else 1f,
        label = "TimerAlpha"
    )

    Text(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
        text = timeText,
        color = textColor,
        fontSize = 50.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
    )
}
