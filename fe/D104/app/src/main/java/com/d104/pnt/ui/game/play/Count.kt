package com.d104.pnt.ui.game.play

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.theme.PixelFont

@Composable
fun Count(
    remainingSeconds: Int,
    themeColor: Color = Color.White
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = String.format("%02d:%02d", minutes, seconds)

    val infinite = rememberInfiniteTransition(label = "siren")

    Row(verticalAlignment = Alignment.CenterVertically) {
        // 왼쪽 경광등
        Box(Modifier.size(6.dp, 40.dp).background(Color.Red).border(1.dp, Color.White))

        Spacer(Modifier.width(16.dp))

        // 중앙 타이머
        Text(
            text = timeText,
            color = themeColor,
            fontSize = 60.sp,
            fontFamily = PixelFont,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.width(16.dp))

        // 오른쪽 경광등
        Box(Modifier.size(6.dp, 40.dp).background(Color.Blue).border(1.dp, Color.White))
    }
}