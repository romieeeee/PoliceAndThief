package com.d104.pnt.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.d104.pnt.R
import com.d104.pnt.ui.theme.MissionYellow
import com.d104.pnt.ui.theme.PixelFont
import com.d104.pnt.ui.theme.PoliceBlue

@Composable
fun WarningOverlay(
    modifier: Modifier = Modifier,
    onWarning: Boolean = false,
    success: Boolean = true,
    warningTitle: String = "",
    warningMessage: String = ""
) {
    val infiniteTransition = rememberInfiniteTransition(label = "warning")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    if (onWarning) {
        Image(
            painter = painterResource(id = R.drawable.warning_overlay),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = modifier
                .fillMaxSize()
                .alpha(alphaAnim)
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedText(
                    modifier = Modifier
                        .alpha(alphaAnim)
                        .fillMaxWidth(),
                    text = warningTitle,
                    fontSize = 40.sp,
                    success = false
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedText(
                    modifier = Modifier
                        .alpha(alphaAnim)
                        .fillMaxWidth(),
                    text = warningMessage,
                    fontSize = 32.sp,
                    success = false
                )
            }
        }
    }
    else { // 알림 메세지
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OutlinedText(
                    modifier = Modifier
                        .alpha(alphaAnim)
                        .fillMaxWidth(),
                    text = warningTitle,
                    fontSize = 40.sp,
                    success = success
                )

                Spacer(modifier = Modifier.height(50.dp))

                OutlinedText(
                    modifier = Modifier
                        .alpha(alphaAnim)
                        .fillMaxWidth(),
                    text = warningMessage,
                    fontSize = 40.sp,
                    success = success
                )
            }
        }
    }
}