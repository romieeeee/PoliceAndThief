package com.d104.pnt.ui.component

import android.graphics.RadialGradient
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.d104.pnt.R
import kotlin.math.max

@Composable
fun WarningOverlay (
    modifier: Modifier = Modifier,
    onWarning: Boolean = false
) {
    if (!onWarning) return

    val infiniteTransition = rememberInfiniteTransition(label = "warning")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing), // 0.5초 간격
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Image(
        painter = painterResource(id = R.drawable.warning_overlay), // 붉은 테두리 PNG
        contentDescription = null,
        // ⭐️ 중요: 화면 비율에 맞춰 이미지를 억지로 늘림 (가장자리가 약간 찌그러져도 자연스러운 이미지여야 함)
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .fillMaxSize()
            .alpha(alphaAnim) // 투명도 애니메이션 적용
    )
}

@Preview
@Composable
fun PreviewWarning(){
    WarningOverlay(
        onWarning = true
    )
}