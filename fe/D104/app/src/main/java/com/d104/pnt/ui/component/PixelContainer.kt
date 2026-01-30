package com.d104.pnt.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.d104.pnt.ui.theme.BorderDefault
import com.d104.pnt.ui.theme.TextPrimary


@Composable
fun PixelContainer(
    modifier: Modifier = Modifier,
    borderWidth: Float = 10f, // 테두리 두께
    cornerSize: Float = 20f, // 모서리 깎이는 정도
    backgroundColor: Color = TextPrimary,
    borderColor: Color = BorderDefault,
    innerHorizontalPadding: Int = 16,      // 내부 좌우 패딩
    innerVerticalPadding: Int = 12,        // 내부 상하 패딩
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .drawBehind {
                val w = size.width
                val h = size.height

                // 픽셀 아트 스타일의 '깎인 모서리' 경로 생성
                val path = Path().apply {
                    moveTo(cornerSize, 0f)
                    lineTo(w - cornerSize, 0f)
                    for (px in borderWidth.toInt() until cornerSize.toInt() + 1 step borderWidth.toInt()) {
                        lineTo(w - cornerSize + px - borderWidth, px.toFloat())
                        lineTo(w - cornerSize + px, px.toFloat())
                    }
                    lineTo(w, h - cornerSize)
                    for (px in borderWidth.toInt() until cornerSize.toInt() + 1 step borderWidth.toInt()) {
                        lineTo(w - px, h - cornerSize + px - borderWidth)
                        lineTo(w - px, h - cornerSize + px)
                    }
                    lineTo(cornerSize, h)
                    for (px in borderWidth.toInt() until cornerSize.toInt() + 1 step borderWidth.toInt()) {
                        lineTo(cornerSize - px + borderWidth, h - px)
                        lineTo(cornerSize - px, h - px)
                    }
                    lineTo(0f, cornerSize)
                    for (px in borderWidth.toInt() until cornerSize.toInt() + 1 step borderWidth.toInt()) {
                        lineTo(px.toFloat(), cornerSize - px + borderWidth)
                        lineTo(px.toFloat(), cornerSize - px)
                    }
                    close()
                }

                // 배경 색칠
                drawPath(path, color = backgroundColor)

                // 테두리 그리기
                drawPath(
                    path,
                    color = borderColor,
                    style = Stroke(width = borderWidth)
                )
            }
            .padding(
                horizontal = innerHorizontalPadding.dp,
                vertical = innerVerticalPadding.dp
            ) // 내부 콘텐츠와 테두리 간격
    ) {
        content()
    }
}