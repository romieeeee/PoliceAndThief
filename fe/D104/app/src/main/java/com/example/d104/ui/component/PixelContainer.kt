package com.example.d104.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.d104.ui.theme.*


@Composable
fun PixelContainer(
    modifier: Modifier = Modifier,
    borderWidth: Float = 5f, // 테두리 두께 (픽셀 느낌)
    cornerSize: Float = 20f, // 모서리 깎이는 정도
    backgroundColor: Color = PixelWhite,
    borderColor: Color = PixelBlack,
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
                    for (px in borderWidth.toInt() until cornerSize.toInt()+1 step borderWidth.toInt()){
                        lineTo(w - cornerSize + px - borderWidth, px.toFloat())
                        lineTo(w - cornerSize + px, px.toFloat())
                    }
//                    lineTo(w, cornerSize)
                    lineTo(w, h - cornerSize) // 그림자 공간 확보를 위해 약간 위에서 멈춤 (선택사항)
                    for (px in borderWidth.toInt() until cornerSize.toInt()+1 step borderWidth.toInt()){
                        lineTo(w - px, h - cornerSize + px - borderWidth)
                        lineTo(w - px, h - cornerSize + px)
                    }
//                    lineTo(w - cornerSize, h)
                    lineTo(cornerSize, h)
                    for (px in borderWidth.toInt() until cornerSize.toInt()+1 step borderWidth.toInt()){
                        lineTo(cornerSize - px + borderWidth, h - px)
                        lineTo(cornerSize - px, h - px)
                    }
//                    lineTo(0f, h - cornerSize)
                    lineTo(0f, cornerSize)
                    for (px in borderWidth.toInt() until cornerSize.toInt()+1 step borderWidth.toInt()){
                        lineTo(px.toFloat(), cornerSize - px + borderWidth)
                        lineTo(px.toFloat(), cornerSize - px)
                    }
                    close()
                }

                // 1. 배경 색칠 (흰색)
                drawPath(path, color = backgroundColor)

                // 2. 테두리 그리기 (검은색)
                drawPath(
                    path,
                    color = borderColor,
                    style = Stroke(width = borderWidth)
                )

                // 3. (선택) 입체감을 위한 우측/하단 그림자 디테일 추가
                // 픽셀 아트 특유의 두꺼운 그림자 라인을 추가합니다.
                val shadowOffset = borderWidth / 2
                drawLine(
                    color = borderColor,
                    start = Offset(cornerSize, h - shadowOffset),
                    end = Offset(w - cornerSize, h - shadowOffset),
                    strokeWidth = borderWidth * 2 // 아래쪽을 더 두껍게
                )
                drawLine(
                    color = borderColor,
                    start = Offset(w - shadowOffset, cornerSize),
                    end = Offset(w - shadowOffset, h - cornerSize),
                    strokeWidth = borderWidth * 2 // 오른쪽을 더 두껍게
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp) // 내부 콘텐츠와 테두리 간격
    ) {
        content()
    }
}