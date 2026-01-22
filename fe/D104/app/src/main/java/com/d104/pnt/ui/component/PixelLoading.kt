package com.d104.pnt.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import com.d104.pnt.R
import androidx.compose.ui.unit.sp
import com.d104.pnt.ui.theme.PixelFont
import androidx.compose.ui.unit.TextUnit

@Composable
fun PixelLoading(
    modifier: Modifier = Modifier,
    size: Int = 100,
    message: String = "LOADING...",
    fontSize: TextUnit = 14.sp,
    textColor: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_transition")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)  // 숫자 커질수록 느리게 회전
        ),
        label = "loading_angle"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_loading_pixel),
            contentDescription = "로딩 중",
            modifier = Modifier
                .size(size.dp)
                .rotate(angle)
        )

        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = message,
                fontSize = fontSize,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontFamily = PixelFont
            )
        }
    }

}

@Preview(showBackground = true)
@Composable
fun PixelLoadingPreview() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        PixelLoading(
            size = 32,
            message = "Loading...",
            fontSize = 10.sp,
            textColor = Color.White
        )
    }
}