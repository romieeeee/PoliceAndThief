package com.d104.pnt.ui.game.wait

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.R
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.theme.AccentYellow
import com.d104.pnt.ui.theme.PixelFont
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

// UI 테스트용 더미 데이터
data class WaitingPlayer(
    val id: Long,
    val nickname: String,
    val role: GameRole,
    val isReady: Boolean = false,
    val isChangingRole: Boolean = false,
    val profileUrl: String? = null
)

@Composable
fun GameWaitingScreen(
    roomId: Long,
    onStartGame: (Long, GameRole) -> Unit = { _, _ -> },
    viewModel: GameWaitingViewModel = hiltViewModel(),
    onBackPressed: () -> Boolean = { false },
    onNavigateHome: () -> Unit = { }
) {
    // ViewModel 상태 구독
    val players by viewModel.players.collectAsStateWithLifecycle()
    val roomInfo by viewModel.roomInfo.collectAsStateWithLifecycle()
    val isHost by viewModel.isHost.collectAsStateWithLifecycle()
    val isMeReady by viewModel.isMeReady.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()

    var showSettingsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is GameWaitingUiEvent.NavigateToHome -> {
                    if (!onBackPressed()) {
                        onNavigateHome()
                    }
                }
            }
        }
    }

    // 게임 시작 성공 시 화면 이동 처리
    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success -> {
                // 게임 시작 성공 시
                onStartGame(roomId, GameRole.POLICE)
            }
            is UiState.Error -> {

            }
            else -> {}
        }
    }

    // 기존 UI 로직 (그대로 유지하되, 데이터 소스만 교체)
    val policeCount = players.count { it.role == GameRole.POLICE && !it.isChangingRole }
    val thiefCount = players.count { it.role == GameRole.THIEF && !it.isChangingRole }
    val isAllReady = players.isNotEmpty() && players.all { it.isReady && !it.isChangingRole }

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
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 헤더(인원수, 시간, 환경설정)
            WaitingHeaderSection(
                roomCode = roomInfo.roomCode,
                currentCount = players.size,
                maxCount = roomInfo.maxCount,
                timeLeft = "${roomInfo.timeLimit}:00",
                isHost = isHost,

                onSettingsClick = { showSettingsDialog = true },
                onLeaveClick = { viewModel.leaveRoom() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 맵 + 인원 + 리스트
            UnifiedWaitingInfoCard(
                players = players,
                myMemberId = myMemberId,
                policeCount = policeCount,
                thiefCount = thiefCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onChangeRole = { viewModel.changeRole() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 준비 버튼
            if (isHost) {
                // 방장
                // 모두 준비되면 흰색, 아니면 회색
                val buttonColor = if (isAllReady) Color.White else Color.Gray
                val borderColor = if (isAllReady) Color.Black else Color.DarkGray

                PixelIconButton(
                    onClick = { if (isAllReady) viewModel.startGame() }, // 준비 안되면 클릭 무시
                    modifier = Modifier.fillMaxWidth(),
                    mainColor = buttonColor,
                    borderColor = borderColor,
                    pixelSize = 3.5.dp,
                    blockHeight = 16,
                    content = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "게임 시작",
                                fontFamily = PixelFont,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                )
            } else {
                val buttonColor = if (isMeReady) Color.Gray else Color.White
                val buttonBorderColor = if (isMeReady) Color.DarkGray else Color.Black
                val textColor = if (isMeReady) Color.White else Color.Black
                val buttonText = if (isMeReady) "준비 취소" else "준비"

                // 참가자
                PixelIconButton(
                    onClick = { viewModel.toggleReady() },
                    modifier = Modifier.fillMaxWidth(),
                    mainColor = buttonColor,
                    borderColor = buttonBorderColor,
                    pixelSize = 3.5.dp,
                    blockHeight = 16,
                    content = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = buttonText,
                                fontFamily = PixelFont,
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(60.dp))
        }

        if (showSettingsDialog) {
            GameSettingsDialog(
                initialState = roomInfo,
                onDismiss = { showSettingsDialog = false },
                onUpdateSettings = { total, time, mission, cctv, police ->
                    viewModel.updateRoomSettings(total, time, mission, cctv, police)
                }
            )
        }
    }
}

// 헤더 섹션
@Composable
fun WaitingHeaderSection(
    roomCode: String,
    currentCount: Int,
    maxCount: Int,
    timeLeft: String,
    isHost: Boolean,
    onSettingsClick: () -> Unit,
    onLeaveClick: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "방 나가기",
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onLeaveClick() }
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = roomCode,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AccentYellow,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 2. 오른쪽: 인원 + 시간 + 설정 아이콘 묶음
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp) // 아이템 간 간격
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
                if (isHost) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "설정",
                        tint = Color(0xFF6591E9),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onSettingsClick() }
                    )
                } else {
                    // 아이콘 자리만큼 공간 확보하거나 생략 (여기선 생략)
                }
            }
        }
    }
}

// 맵, 인원수, 참가자 리스트
@Composable
fun UnifiedWaitingInfoCard(
    players: List<WaitingPlayer>,
    myMemberId: Long,
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
                    PlayerSlotCard(
                        player = player,
                        isMe = (player.id == myMemberId)
                    )
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
fun PlayerSlotCard(
    player: WaitingPlayer,
    isMe: Boolean
) {
    val borderColor = when {
        player.isChangingRole -> Color(0xFFFF5252)
        player.isReady -> Color(0xFF76FF03)
        else -> Color(0xFF8D90B3)
    }

    val cardBackgroundColor = if (isMe) Color(0xFFE3F2FD) else Color.White

    val nicknameColor = if (isMe) Color(0xFF1565C0) else Color.Black // 나는 파란색 닉네임
    val nicknameWeight = if (isMe) FontWeight.Bold else FontWeight.Normal

    val roleIcon = if (player.isChangingRole) "?" else if (player.role == GameRole.POLICE) "👮" else "🕵️"

    PixelContainer(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        backgroundColor = cardBackgroundColor,
        borderColor = borderColor,
        borderWidth = 5f,
        cornerSize = 8f
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF59D))
            )
            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = player.nickname,
                style = MaterialTheme.typography.bodySmall,
                color = nicknameColor,
                fontWeight = nicknameWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = roleIcon,
                fontSize = 18.sp,
                fontWeight = if (player.isChangingRole) FontWeight.Bold else FontWeight.Normal,
                color = if (player.isChangingRole) Color.Red else Color.Black
            )
        }
    }
}