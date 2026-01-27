package com.d104.pnt.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.d104.pnt.R
import com.d104.pnt.data.remote.model.response.ProfileResponse
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.ui.component.PixelContainer

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileState by viewModel.profileState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.bg_night),
            contentDescription = "배경 화면",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 상태에 따라 다른 화면 보여주기
        when (val state = profileState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is UiState.Success -> {
                // 데이터 로드 성공 시 ProfileContent 호출
                ProfileContent(
                    profile = state.data,
                    onLogoutClick = { /* TODO: 로그아웃 로직 연결 */ }
                )
            }
            is UiState.Error -> {
                // 에러 발생 시 메시지 표시
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "정보를 불러오지 못했습니다.\n${state.message}",
                        color = Color.Red
                    )
                }
            }
            else -> {} // Idle 상태
        }
    }
}

@Composable
fun ProfileContent(
    profile: ProfileResponse,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.weight(0.2f))

        // 프로필 카드 (닉네임, 아이디 등)
        ProfileCardSection(
            nickname = profile.nickname,
            loginId = profile.loginId,
            avatarUrl = profile.avatarUrl,
            onLogoutClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .weight(0.8f)
        )

        // 전적 요약 (승률 그래프)
        StatSummarySection(
            wins = profile.stat.wins,
            totalGames = profile.stat.totalGames,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .weight(0.5f)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 등급 섹션
        GradeSection(
            policeGrade = profile.stat.policeGrade,
            thiefGrade = profile.stat.thiefGrade,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .weight(0.6f)
        )

        Spacer(modifier = Modifier.weight(0.6f))
    }
}

@Composable
fun ProfileCardSection(
    nickname: String,
    loginId: String,
    avatarUrl: String?,
    onLogoutClick: () -> Unit,
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
            Box(modifier = Modifier.size(100.dp)) {
                // TODO: Coil 라이브러리 추가 시 AsyncImage로 교체
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
                    modifier = Modifier.offset(y = (-5).dp)
                ) {
                    Text(
                        text = "이름: $nickname",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "수정 아이콘",
                        modifier = Modifier
                            .size(30.dp)
                            .clickable { /* 수정 화면 이동 */ }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.offset(y = (-5).dp)) {
                    Text(
                        text = "아이디:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = loginId,
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
                .padding(bottom = 40.dp, end = 24.dp)
                .clickable { onLogoutClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "로그아웃", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "→", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StatSummarySection(
    wins: Int,
    totalGames: Int,
    modifier: Modifier = Modifier
) {
    val winRate = if (totalGames > 0) (wins.toFloat() / totalGames) * 100 else 0f
    val activeBlocks = if (totalGames > 0) ((winRate / 100) * 8).toInt() else 0

    PixelContainer(
        modifier = modifier,
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
            Text(text = "전적 요약", color = Color.White, fontSize = 14.sp)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "승률 (${winRate.toInt()}%)",
                    color = Color(0xFFC4C4C4),
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                Text(text = "플레이 횟수", color = Color(0xFFC4C4C4), fontSize = 12.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "🎮", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$totalGames",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun GradeSection(
    policeGrade: String,
    thiefGrade: String,
    modifier: Modifier = Modifier
) {
    PixelContainer(
        modifier = modifier,
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
                GradeItem(
                    title = "경찰",
                    grade = policeGrade,
                    iconRes = R.drawable.img_tier_police,
                    modifier = Modifier.weight(1f)
                )

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

@Composable
fun GradeItem(
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
        Text(text = title, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "$title 티어",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(80.dp)
        )
        Text(
            text = grade,
            color = Color(0xFFA3E946),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}