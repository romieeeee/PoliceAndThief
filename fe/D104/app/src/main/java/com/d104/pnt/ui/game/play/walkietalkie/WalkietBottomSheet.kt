package com.d104.pnt.ui.game.play.walkietalkie

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d104.pnt.R
import com.d104.pnt.ui.game.play.mission.BottomSheetState
import com.d104.pnt.ui.theme.PixelFont
import kotlin.math.roundToInt

@Composable
fun WalkieBottomSheet(
    modifier: Modifier = Modifier,
    onSheetStateChanged: ((BottomSheetState) -> Unit)? = null,
    isSomeoneTalking: Boolean = false,
    channel: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    var sheetState by remember { mutableStateOf(BottomSheetState.COLLAPSED) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var imageHeight by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

    val collapsedOffset = screenHeight - 400f // 윗부분만 보이는 높이
    val expandedOffset = screenHeight * 0.28f // 전체가 보이는 높이

    val targetOffset = when (sheetState) {
        BottomSheetState.COLLAPSED -> collapsedOffset
        BottomSheetState.EXPANDED -> expandedOffset
    }

    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset + dragOffset,
        animationSpec = tween(durationMillis = 300)
    )

    LaunchedEffect(sheetState) {
        onSheetStateChanged?.invoke(sheetState)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .offset { IntOffset(0, animatedOffset.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            dragOffset = 0f
                        },
                        onDragEnd = {
                            val currentOffset = targetOffset + dragOffset
                            val threshold = (collapsedOffset + expandedOffset) / 2

                            sheetState = if (currentOffset < threshold) {
                                BottomSheetState.EXPANDED
                            } else {
                                BottomSheetState.COLLAPSED
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            dragOffset = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newDragOffset = dragOffset + dragAmount.y
                            val newOffset = targetOffset + newDragOffset

                            if (newOffset in expandedOffset..collapsedOffset) {
                                dragOffset = newDragOffset
                            }
                        }
                    )
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    sheetState = when (sheetState) {
                        BottomSheetState.COLLAPSED -> BottomSheetState.EXPANDED
                        else -> BottomSheetState.EXPANDED
                    }
                }
        ) {
            // 배경 이미지
            Image(
                painter = painterResource(id = R.drawable.walkie_talkie),
                contentDescription = "walkie talkie",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.45f)
                    .onGloballyPositioned { coordinates ->
                        imageHeight = coordinates.size.height.toFloat()
                    },
                contentScale = ContentScale.FillBounds
            )


            // Expanded 상태의 전체 컨텐츠
            if (imageHeight > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(
                                top = with(density) { (imageHeight * 0.2f).toDp() },
                                start = 60.dp,
                                end = 60.dp,
                                bottom = 32.dp
                            )
                            .systemBarsPadding(),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        content()
                    }

                    if (sheetState == BottomSheetState.COLLAPSED && isSomeoneTalking && imageHeight > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(
                                    top = with(density) { (imageHeight * 0.05f).toDp() },
                                    start = 40.dp,
                                    end = 40.dp
                                )
                        ) {
                            WalkieReceivingIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalkieReceivingIndicator() {
//    val infiniteTransition = rememberInfiniteTransition(label = "wave")
//    val scale by infiniteTransition.animateFloat(
//        initialValue = 1f,
//        targetValue = 1.3f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(800),
//            repeatMode = RepeatMode.Reverse
//        ),
//        label = "scale"
//    )
//
//    Box(
//        modifier = Modifier.fillMaxWidth(),
//        contentAlignment = Alignment.Center
//    ) {
//        // 외곽 원 (파동 효과)
//        Box(
//            modifier = Modifier
//                .size(50.dp)
//                .scale(scale)
//                .background(
//                    Color(0xFFFFA500).copy(alpha = 0.3f),
//                    CircleShape
//                )
//        )
//
//        // 중앙 아이콘
//        Box(
//            modifier = Modifier
//                .size(40.dp)
//                .background(Color(0xFFFFA500), CircleShape),
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(
//                imageVector = Icons.Default.Mic,
//                contentDescription = null,
//                tint = Color.White,
//                modifier = Modifier.size(20.dp)
//            )
//        }
//    }
}
