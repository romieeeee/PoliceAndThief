package com.d104.pnt.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.d104.pnt.ui.theme.*

@Composable
fun PixelDashedDivider(
    color: Color = BorderDefault,
    thickness: Dp = 2.dp, // 선 두께
    dashLength: Dp = 4.dp, // 점 길이
    gapLength: Dp = 4.dp   // 점 사이 간격
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(thickness)
    ) {
        // 점선 효과 정의 (점 길이, 간격)
        val pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), gapLength.toPx()),
            0f
        )

        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = thickness.toPx(),
            pathEffect = pathEffect
        )
    }
}