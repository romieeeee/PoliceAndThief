package com.d104.pnt.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.d104.pnt.R
import com.d104.pnt.ui.component.PixelContainer

@Composable
fun ProfileCardSection(
    nickname: String,
    avatarUrl: String?,
    policeGrade: String, // ✅ 추가: 경찰 등급
    thiefGrade: String,  // ✅ 추가: 도둑 등급
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
    onUpdateNickname: (String) -> Unit = {},
    onUpdateAvatar: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedNickname by remember { mutableStateOf(nickname) }

    LaunchedEffect(nickname) {
        editedNickname = nickname
    }

    // 전체를 감싸는 큰 컨테이너
    PixelContainer(
        modifier = modifier,
        backgroundColor = Color(0xFF3F3F68),
        borderColor = Color(0xFF8D90B3),
        borderWidth = 8f,
        cornerSize = 30f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. 상단: 아바타 + 닉네임 + 편집 버튼 ---
            Box(
                contentAlignment = Alignment.TopEnd // 편집 아이콘 위치 잡기 위함
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 아바타 사진 (원형)
                    Box(
                        modifier = Modifier
                            .size(100.dp) // 크기 키움
                            .clip(CircleShape)
                            .border(3.dp, Color.Black, CircleShape)
                            .background(Color.White) // 배경 흰색 (투명 이미지 대비)
                            .clickable { onUpdateAvatar() } // 사진 눌러도 변경 가능하게
                    ) {
                        val painter = when (avatarUrl) {
                            "POLICE_1" -> painterResource(id = R.drawable.profile_img_police_1)
                            "POLICE_2" -> painterResource(id = R.drawable.profile_img_police_2)
                            "THIEF_1" -> painterResource(id = R.drawable.profile_img_thief_1)
                            "THIEF_2" -> painterResource(id = R.drawable.profile_img_thief_2)
                            "DEFAULT", null, "" -> painterResource(id = R.drawable.profile_img_default)
                            else -> rememberAsyncImagePainter(
                                model = avatarUrl,
                                error = painterResource(id = R.drawable.profile_img_default),
                                placeholder = painterResource(id = R.drawable.profile_img_default)
                            )
                        }

                        Image(
                            painter = painter,
                            contentDescription = "나의 아바타",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 닉네임 (중앙 정렬)
                    if (isEditing) {
                        BasicTextField(
                            value = editedNickname,
                            onValueChange = { if (it.length <= 10) editedNickname = it },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            modifier = Modifier
                                .width(140.dp)
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(4.dp),
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = nickname,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 편집(연필) 아이콘 (아바타 오른쪽 위)
                Image(
                    painter = painterResource(id = if (isEditing) R.drawable.check else R.drawable.ic_edit),
                    contentDescription = "편집",
                    modifier = Modifier
                        .offset(x = 30.dp, y = 0.dp) // 위치 미세 조정
                        .size(24.dp)
                        .clickable {
                            if (isEditing) {
                                if (editedNickname.isNotBlank() && editedNickname != nickname) {
                                    onUpdateNickname(editedNickname)
                                }
                                isEditing = false
                            } else {
                                editedNickname = nickname
                                isEditing = true
                            }
                        }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 2. 구분선 ---
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                thickness = 2.dp,
                color = Color(0xFF293D36) // 어두운 녹색 계열 구분선
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- 3. 하단: 경찰/도둑 등급 (좌우 배치) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 경찰 등급
                GradeItem(
                    title = "경찰",
                    grade = policeGrade,
                    iconRes = R.drawable.img_tier_police,
                    modifier = Modifier.weight(1f)
                )

                // 중앙 점선 (세로)
                Canvas(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp)
                ) {
                    drawLine(
                        color = Color(0xFF8D90B3),
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // 도둑 등급
                GradeItem(
                    title = "도둑",
                    grade = thiefGrade,
                    iconRes = R.drawable.img_tier_thief,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// 등급 아이템 (내부에서 재사용)
@Composable
private fun GradeItem(
    title: String,
    grade: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, color = Color(0xFFC4C4C4), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // 티어 아이콘 박스
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFF233342), RoundedCornerShape(8.dp))
                .border(2.dp, Color(0xFF6591E9), RoundedCornerShape(8.dp)), // 파란 테두리
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "$title 티어",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp) // 아이콘 크기
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = grade,
            color = Color(0xFFA3E946), // 형광 연두
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}