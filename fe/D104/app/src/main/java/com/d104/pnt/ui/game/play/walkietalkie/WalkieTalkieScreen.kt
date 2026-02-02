package com.d104.pnt.ui.game.play.walkietalkie

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import timber.log.Timber

object WalkieColor {
    val Panel = Color(0xFF2A2A2A)
    val Button = Color(0xFF3A3A3A)
    val TextPrimary = Color(0xFFE0E0E0)
    val Danger = Color(0xFFE53935)
}

/**
 * 무전기 UI (내부용)
 */
@Composable
fun WalkieTalkieContent(
    channel: String,
    isTalking: Boolean,
    isSomeoneTalking: Boolean,
    onPttDown: () -> Unit,
    onPttUp: () -> Unit,
    onChannelDown: () -> Unit,
    onChannelUp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        ChannelHeader(channel)

        PttCircle(
            isTalking = isTalking,
            isSomeoneTalking = isSomeoneTalking,
            onDown = onPttDown,
            onUp = onPttUp
        )

        ChannelButtons(
            onDown = onChannelDown,
            onUp = onChannelUp
        )
    }
}

@Composable
private fun ChannelHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(WalkieColor.Panel, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = WalkieColor.TextPrimary,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PttCircle(
    isTalking: Boolean,
    isSomeoneTalking: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.06f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "ptt-scale"
    )

    val circleColor = when {
        isTalking -> WalkieColor.Danger // 내가 말하는 중 - 빨강
        isSomeoneTalking -> Color(0xFFFFA500) // 다른 사람 말하는 중 - 주황
        else -> WalkieColor.Panel // 대기 중 - 회색
    }

    val displayText = when {
        isTalking -> "송신 중…"
        isSomeoneTalking -> "다른 경찰 송신 중"
        else -> "누르고 말하세요"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 24.dp)
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(isSomeoneTalking) {
                detectTapGestures(
                    onPress = {
                        if (isSomeoneTalking) {
                            Timber.d("📻 다른 경찰 송신 중 - 버튼 무시")
                            return@detectTapGestures
                        }

                        isPressed = true
                        onDown()

                        tryAwaitRelease()

                        isPressed = false
                        onUp()
                    }
                )
            }
            .background(
                color = circleColor,
                shape = CircleShape
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = WalkieColor.TextPrimary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = displayText,
            color = WalkieColor.TextPrimary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ChannelButtons(
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ChannelButton("CH ▼", onDown)
        ChannelButton("CH ▲", onUp)
    }
}

@Composable
private fun ChannelButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(WalkieColor.Button, RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 20.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = WalkieColor.TextPrimary)
    }
}