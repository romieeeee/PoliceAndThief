package com.d104.pnt.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun TierGuideDialog(
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp) // 높이 고정 (스크롤 사용 위해)
                .background(Color(0xFF232323), RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF8D90B3), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 제목
                Text(
                    text = "계급 시스템 가이드",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 설명 멘트
                Text(
                    text = "경기를 승리하면 승급하고,\n패배하면 강등됩니다.",
                    color = Color(0xFFC4C4C4),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 계급표 헤더
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "경찰",
                        modifier = Modifier.weight(1f),
                        color = Color(0xFF6591E9),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "도둑",
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFFF6B6B), // 도둑은 붉은 계열 추천 (혹은 테마색)
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.Gray, thickness = 1.dp)

                // 계급 리스트 (스크롤 가능)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val policeRanks = listOf(
                        "순경", "경장", "경사", "경위", "경감", "경정",
                        "총경", "경무관", "치안감", "치안정감", "치안총감"
                    )
                    val thiefRanks = listOf(
                        "바늘도둑", "좀도둑", "소매치기", "빈집털이", "소도둑", "금고털이",
                        "은행털이", "홍길동", "인비저블", "괴도", "대도"
                    )

                    // 11단계 표시
                    for (i in 0 until 11) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = policeRanks.getOrElse(i) { "" },
                                modifier = Modifier.weight(1f),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )

                            // 중앙 화살표나 구분
                            Text(
                                text = "Lv.${i + 1}",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )

                            Text(
                                text = thiefRanks.getOrElse(i) { "" },
                                modifier = Modifier.weight(1f),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                        if (i < 10) {
                            HorizontalDivider(color = Color(0xFF3F3F3F), thickness = 1.dp)
                        }
                    }
                }
            }

            // 닫기 버튼 (우측 상단)
            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-12).dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = Color.White
                )
            }
        }
    }
}