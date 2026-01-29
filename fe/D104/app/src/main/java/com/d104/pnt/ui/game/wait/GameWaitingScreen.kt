package com.d104.pnt.ui.game.wait

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d104.pnt.R
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.theme.PixelFont
import androidx.compose.material3.MaterialTheme
import com.d104.pnt.ui.theme.AccentYellow

// UI 테스트용 더미 데이터
data class WaitingPlayer(
    val id: Long,
    val nickname: String,
    val role: GameRole,
    val profileUrl: String? = null
)

@Composable
fun GameWaitingScreen(
    roomId: Long,
    roomCode: String = "ENTRY1",
    players: List<WaitingPlayer> = List(30) {
        WaitingPlayer(
            it.toLong(),
            "참가자 ${it + 1}",
            if (it % 3 == 0) GameRole.POLICE else GameRole.THIEF
        )
    },
    onStartGame: (Long, GameRole) -> Unit = { _, _ -> },
    onChangeRole: () -> Unit = {},
    onBackPressed: () -> Boolean = { false }
) {
    val policeCount = players.count { it.role == GameRole.POLICE }
    val thiefCount = players.count { it.role == GameRole.THIEF }

    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.bg_playground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 헤더(인원수, 시간, 환경설정)
            WaitingHeaderSection(
                roomCode = roomCode,
                currentCount = players.size,
                maxCount = 30,
                timeLeft = "30:00"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 맵 + 인원 + 리스트
            UnifiedWaitingInfoCard(
                players = players,
                policeCount = policeCount,
                thiefCount = thiefCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onChangeRole = onChangeRole
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 준비 버튼
            PixelIconButton(
                onClick = { /* 준비 완료 로직 */ },
                modifier = Modifier.fillMaxWidth(),
                mainColor = Color.White,
                borderColor = Color.Black,
                pixelSize = 3.5.dp,
                blockHeight = 16,
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "준비",
                            fontFamily = PixelFont,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// 헤더 섹션
@Composable
fun WaitingHeaderSection(
    roomCode: String,
    currentCount: Int,
    maxCount: Int,
    timeLeft: String
) {
    PixelContainer(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF2D3242),
        borderColor = Color(0xFF6591E9),
        borderWidth = 4f,
        cornerSize = 20f,
        innerHorizontalPadding = 4
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 왼쪽: 방 코드
            Text(
                text = roomCode,
                fontFamily = PixelFont,
                color = AccentYellow,
                fontSize = 20.sp, // 조금 크게 강조
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // 2. 오른쪽: 인원 + 시간 + 설정 아이콘 묶음
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp) // 아이템 간 간격
            ) {
                // 인원수
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$currentCount/$maxCount",
                        fontFamily = PixelFont,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                // 시간
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeLeft,
                        fontFamily = PixelFont,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }

                // 설정 아이콘
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color(0xFF6591E9),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// 맵, 인원수, 참가자 리스트
@Composable
fun UnifiedWaitingInfoCard(
    players: List<WaitingPlayer>,
    policeCount: Int,
    thiefCount: Int,
    modifier: Modifier = Modifier,
    onChangeRole: () -> Unit
) {
    PixelContainer(
        modifier = modifier,
        backgroundColor = Color(0xFF2D3242),
        borderColor = Color(0xFF6591E9),
        borderWidth = 4f,
        cornerSize = 20f,
        innerHorizontalPadding = 2,
        innerVerticalPadding = 12
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 상단: 맵 프리뷰 및 인원수 정보
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MapPreviewContent()

                Spacer(modifier = Modifier.height(16.dp))

                RoleCountInfo(policeCount, thiefCount)

                Spacer(modifier = Modifier.height(20.dp))

                // 구분선
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFF6591E9).copy(alpha = 0.5f))
                )
            }

            // 하단: 참가자 리스트
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(players) { player ->
                    PlayerSlotCard(player = player)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                PixelIconButton(
                    onClick = onChangeRole,
                    modifier = Modifier.width(90.dp),
                    pixelSize = 2.dp,
                    blockHeight = 16,
                    content = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "역할 변경",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Black
                            )
                        }
                    }
                )
            }
        }
    }
}

// 맵 프리뷰 내용
@Composable
fun MapPreviewContent() {
    Image(
        painter = painterResource(id = R.drawable.img_map_example),
        contentDescription = "맵 프리뷰",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(width = 130.dp, height = 130.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, Color(0xFF6591E9), RoundedCornerShape(8.dp))
    )
}

// 진영별 인원수 정보
@Composable
fun RoleCountInfo(policeCount: Int, thiefCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "👮", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$policeCount", fontFamily = PixelFont, color = Color.White, fontSize = 20.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🕵️", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$thiefCount", fontFamily = PixelFont, color = Color.White, fontSize = 20.sp)
        }
    }
}

// 참가자 슬롯 카드
@Composable
fun PlayerSlotCard(player: WaitingPlayer) {
    PixelContainer(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        backgroundColor = Color.White,
        borderColor = Color(0xFF8D90B3),
        borderWidth = 2f,
        cornerSize = 8f
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
//                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF59D))
            ) {
                // 추후 프로필 이미지 추가
            }
            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = player.nickname,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (player.role == GameRole.POLICE) "👮" else "🕵️",
                fontSize = 18.sp
            )
        }
    }
}

@Preview
@Composable
fun PreviewGameWaitingScreen() {
    GameWaitingScreen(roomId = 1)
}