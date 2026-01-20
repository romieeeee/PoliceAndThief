package com.example.d104.ui.mypage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.d104.R
import com.example.d104.ui.component.PixelContainer

@Composable
fun MyPageScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_night),
            contentDescription = "배경 화면",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.2f))

            ProfileCardSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .weight(0.8f)
            )

            PixelContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .weight(0.5f),
                backgroundColor = Color(0xFF3F3F68),
                borderColor = Color(0xFF8D90B3),
                borderWidth = 8f,
                cornerSize = 30f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        text = "전적 요약",
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "승률 (%)",
                            color = Color(0xFFC4C4C4),
                            fontSize = 12.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val activeBlocks = 5
                            repeat(8) { index ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(18.dp)
                                        .background(
                                            if (index < activeBlocks) Color(0xFFA3E946)
                                            else Color(0xFFD9D9D9)
                                        )
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "플레이 횟수",
                            color = Color(0xFFC4C4C4),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(text = "🎮", fontSize = 16.sp)

                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "168",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PixelContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .weight(0.6f),
                backgroundColor = Color(0xFF3F3F68),
                borderColor = Color(0xFF8D90B3),
                borderWidth = 8f,
                cornerSize = 30f
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(24.dp)
                            .background(Color(0xFF3F3F68))
                            .border(1.dp, Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "경찰",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .offset(x = -8.dp, y = 0.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Image(
                                painter = painterResource(id = R.drawable.img_tier_police),
                                contentDescription = "경찰 티어",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(80.dp)
                                    .offset(x = -8.dp, y = 0.dp)
                            )
                        }

                        Canvas(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight(0.8f)
                        ) {
                            drawLine(
                                color = Color(0xFF8D90B3),
                                start = Offset(0f, 0f),
                                end = Offset(0f, size.height),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f),
                                strokeWidth = 4.dp.toPx()
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "도둑",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .offset(x = 8.dp, y = 0.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Image(
                                painter = painterResource(id = R.drawable.img_tier_thief),
                                contentDescription = "도둑 티어",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(80.dp)
                                    .offset(x = 8.dp, y = 0.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(0.6f))
        }
    }
}

@Composable
fun ProfileCardSection(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = R.drawable.frame_profile),
            contentDescription = "프로필 배경",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, top = 10.dp, end = 24.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

           Box(
                modifier = Modifier
                    .size(100.dp)
                    .offset(x = 0.dp, y = 0.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_avatar),
                    contentDescription = "나의 아바타",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .offset(x = 0.dp, y = -5.dp)
                ) {
                    Text(
                        text = "이름: 이래롬",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "수정 아이콘",
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .offset(x = 0.dp, y = -5.dp)
                ) {
                    Text(
                        text = "생년월일:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "1999.12.25",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 40.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "로그아웃", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "→", fontSize = 12.sp, color = Color.Gray)
        }
    }
}