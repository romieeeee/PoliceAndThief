package com.d104.pnt.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.d104.pnt.R
import com.d104.pnt.ui.theme.PixelFont

@Composable
fun ArrestOverlay(
    modifier: Modifier,
    isVisible: Boolean,
    onAnimationFinished: () -> Unit = {} // 필요시 애니메이션 후처리
) {
    // 체포 상태가 아니면 아예 그리지 않음 (성능 최적화)
    // 단, 애니메이션이 '나가는' 것도 보고 싶다면 if문 대신 AnimatedVisibility를 최상위에 둬야 함
    // 여기서는 "게임 오버"이므로 닫히고 끝나는 구조로 작성합니다.

    // 전체 화면을 덮는 Box
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 1. 터치 차단막 (투명)
        // isArrested가 true일 때만 터치를 먹어버림
        if (isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectTapGestures { }
                        detectDragGestures { _, _ -> } // 드래그도 막음
                    }
            )
        }

        // 2. 위쪽 창살 (위 -> 아래로 내려옴)
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { -it }, // 자기 높이만큼 위에서 시작
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing) // 쿵! 하고 닫히는 느낌
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Image(
                painter = painterResource(id = R.drawable.arrested_overlay_top), // 위쪽 창살 이미지
                contentDescription = null,
                contentScale = ContentScale.Crop, // 가로 꽉 차게
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f) // 화면 절반보다 조금 더 내려오게 (겹치도록)
            )
        }

        // 3. 아래쪽 창살 (아래 -> 위로 올라옴)
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it }, // 자기 높이만큼 아래에서 시작
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Image(
                painter = painterResource(id = R.drawable.arrested_overlay_bottom), // 아래쪽 창살 이미지
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
            )
        }

        // 4. (선택사항) "검거됨" 도장 텍스트
        // 창살이 닫히고 조금 뒤에 찍히는 연출
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(initialScale = 2f, animationSpec = tween(300, delayMillis = 500)) + fadeIn(tween(300, delayMillis = 500)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = "체 포 됨",
                color = Color.Red,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PixelFont,
                modifier = Modifier
                    .rotate(-15f) // 약간 기울여서 도장 찍힌 느낌
                    .border(4.dp, Color.Red, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}