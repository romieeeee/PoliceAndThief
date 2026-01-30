package com.d104.pnt.ui.game.play

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun FlipImage(
    frontRes: Int,
    backRes: Int,
    size: Dp = 280.dp
) {
    val rotation = remember { Animatable(0f) }
    var isFront by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            // 탭으로 플립
            .clickable {
                scope.launch {
                    val target = if (isFront) -180f else 0f
                    rotation.animateTo(
                        target,
                        animationSpec = tween(
                            durationMillis = 450,
                            easing = FastOutSlowInEasing
                        )
                    )
                    isFront = !isFront
                }
            }
            // 드래그로 회전
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, dragAmount ->
                        scope.launch {
                            val newRotation =
                                (rotation.value + dragAmount.x * 0.4f)
                                    .coerceIn(-180f, 0f)

                            rotation.snapTo(newRotation)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            val target =
                                if (rotation.value < -90f) -180f else 0f

                            rotation.animateTo(
                                target,
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = FastOutSlowInEasing
                                )
                            )
                            isFront = target == 0f
                        }
                    }
                )
            }
            .graphicsLayer {
                rotationY = rotation.value
                cameraDistance = 16 * density

                // 동전 얇아지는 느낌
                val absRotation = kotlin.math.abs(rotation.value)
                scaleX = if (absRotation in 70f..110f) 0.92f else 1f
            },
        contentAlignment = Alignment.Center
    ) {
        // 90도 기준으로 이미지 결정
        val showFront = rotation.value > -90f

        Image(
            painter = painterResource(if (showFront) frontRes else backRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
