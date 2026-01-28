package com.d104.pnt.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d104.pnt.ui.component.PixelContainer

@Composable
fun StatSummarySection(
    wins: Int,
    totalGames: Int,
    modifier: Modifier = Modifier
) {
    val winRate = if (totalGames > 0) (wins.toFloat() / totalGames) * 100 else 0f
    // 승률에 따른 게이지 칸 수 (총 10칸 기준)
    val activeBlocks = if (totalGames > 0) (winRate / 10).toInt() else 0

    PixelContainer(
        modifier = modifier,
        backgroundColor = Color(0xFF3F3F68),
        borderColor = Color(0xFF8D90B3),
        borderWidth = 8f,
        cornerSize = 20f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "전적 요약",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            // 초록색 게이지 바
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(10) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp) // 높이 조절
                            .background(
                                if (index < activeBlocks) Color(0xFFA3E946) // 형광 연두 (채워짐)
                                else Color(0xFFD9D9D9) // 회색 (빈칸)
                            )
                    )
                }
            }

            // 하단 텍스트 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "승률 (${winRate.toInt()}%)",
                    color = Color(0xFFC4C4C4),
                    fontSize = 12.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎮", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "플레이 횟수",
                        color = Color(0xFFC4C4C4),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$totalGames",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}