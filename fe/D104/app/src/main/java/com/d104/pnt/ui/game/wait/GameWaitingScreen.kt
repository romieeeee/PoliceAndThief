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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.window.Dialog
import com.d104.pnt.ui.component.UserProfileCard
import androidx.compose.foundation.interaction.MutableInteractionSource

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
    onNavigateHome: (String?) -> Unit = { }
) {
    val players by viewModel.players.collectAsStateWithLifecycle()
    val roomInfo by viewModel.roomInfo.collectAsStateWithLifecycle()
    val isHost by viewModel.isHost.collectAsStateWithLifecycle()
    val isMeReady by viewModel.isMeReady.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()

    var selectedPlayerId by remember { mutableStateOf<Long?>(null) }
    var dismissedPlayerId by remember { mutableStateOf<Long?>(null) }
    var lastDismissTime by remember { mutableLongStateOf(0L) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var infoDialogTarget by remember { mutableStateOf<WaitingPlayer?>(null) }
    var kickDialogTarget by remember { mutableStateOf<WaitingPlayer?>(null) }

    val context = LocalContext.current


    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is GameWaitingUiEvent.NavigateToHome -> {
                    if (!onBackPressed()) {
                        onNavigateHome(event.message)
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is UiState.Success -> {
                onStartGame(roomId, GameRole.POLICE)
            }
            is UiState.Error -> {

            }
            else -> {}
        }
    }

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
            // 인원수, 시간, 환경설정
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

            // 맵, 인원, 리스트
            UnifiedWaitingInfoCard(
                players = players,
                myMemberId = myMemberId,
                policeCount = policeCount,
                thiefCount = thiefCount,
                isHost = isHost,
                selectedPlayerId = selectedPlayerId,
                onPlayerClick = { player ->
                    val now = System.currentTimeMillis()
                    val isJustDismissed = (dismissedPlayerId == player.id) && (now - lastDismissTime < 300)

                    if (!isJustDismissed) {
                        selectedPlayerId = if (selectedPlayerId == player.id) null else player.id
                    }
                },
                onMenuDismiss = {
                    dismissedPlayerId = selectedPlayerId
                    lastDismissTime = System.currentTimeMillis()
                    selectedPlayerId = null
                },
                onInfoClick = { player ->
                    // 메뉴 닫고
                    dismissedPlayerId = selectedPlayerId
                    lastDismissTime = System.currentTimeMillis()
                    selectedPlayerId = null

                    // 다이얼로그 타겟 설정
                    infoDialogTarget = player
                },
                onKickClick = { player ->
                    dismissedPlayerId = selectedPlayerId
                    lastDismissTime = System.currentTimeMillis()
                    selectedPlayerId = null

                    kickDialogTarget = player
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                onChangeRole = { viewModel.changeRole() }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 준비 버튼
            if (isHost) {
                val buttonColor = if (isAllReady) Color.White else Color.Gray
                val borderColor = if (isAllReady) Color.Black else Color.DarkGray

                PixelIconButton(
                    onClick = { if (isAllReady) viewModel.startGame() },
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

        if (infoDialogTarget != null) {
            PlayerInfoDialog(
                player = infoDialogTarget!!,
                onDismiss = { infoDialogTarget = null }
            )
        }

        if (kickDialogTarget != null) {
            KickConfirmDialog(
                player = kickDialogTarget!!,
                onDismiss = { kickDialogTarget = null },
                onConfirm = { reason ->
                    viewModel.kickPlayer(kickDialogTarget!!.id, reason)
                    kickDialogTarget = null
                }
            )
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                // 설정
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
    isHost: Boolean,
    selectedPlayerId: Long?,
    onPlayerClick: (WaitingPlayer) -> Unit,
    onMenuDismiss: () -> Unit,
    onInfoClick: (WaitingPlayer) -> Unit,
    onKickClick: (WaitingPlayer) -> Unit,
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
            // 맵 프리뷰 및 인원수 정보
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

            // 참가자 리스트
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
                        isMe = (player.id == myMemberId),
                        isHost = isHost,
                        showMenu = (player.id == selectedPlayerId),
                        onClick = { onPlayerClick(player) },
                        onDismissMenu = onMenuDismiss,
                        onInfoClick = {
                            onMenuDismiss()
                            onInfoClick(player)
                        },
                        onKickClick = {
                            onMenuDismiss()
                            onKickClick(player)
                        }
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
    isMe: Boolean,
    isHost: Boolean,
    showMenu: Boolean,
    onClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onInfoClick: () -> Unit,
    onKickClick: () -> Unit
) {
    // 테두리 및 배경색 로직
    val borderColor = when {
        player.isChangingRole -> Color(0xFFFF5252)
        player.isReady -> Color(0xFF76FF03)
        else -> Color(0xFF8D90B3)
    }
    val cardBackgroundColor = if (isMe) Color(0xFFE3F2FD) else Color.White
    val nicknameColor = if (isMe) Color(0xFF1565C0) else Color.Black
    val nicknameWeight = if (isMe) FontWeight.Bold else FontWeight.Normal
    val roleIcon = if (player.isChangingRole) "?" else if (player.role == GameRole.POLICE) "👮" else "🕵️"

    Box {
        PixelContainer(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clickable { onClick() },
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

        if (showMenu) {
            PlayerActionMenu(
                isHost = isHost,
                isTargetMe = isMe,
                onDismiss = onDismissMenu,
                onInfoClick = onInfoClick,
                onKickClick = onKickClick
            )
        }
    }
}

// 메뉴 클릭 시
@Composable
fun PlayerActionMenu(
    isHost: Boolean,
    isTargetMe: Boolean,
    onDismiss: () -> Unit,
    onInfoClick: () -> Unit,
    onKickClick: () -> Unit
) {
    val density = LocalDensity.current

    val yOffset = remember(density) {
        with(density) { (48.dp + 4.dp).roundToPx() }
    }

    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, yOffset),
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MenuButton(
                text = "정보 확인",
                textColor = Color.Black,
                onClick = onInfoClick
            )

            if (isHost && !isTargetMe) {
                MenuButton(
                    text = "강퇴하기",
                    textColor = Color(0xFFFF5252),
                    onClick = onKickClick
                )
            }
        }
    }
}

// 메뉴 버튼
@Composable
fun MenuButton(
    text: String,
    textColor: Color,
    onClick: () -> Unit
) {
    PixelContainer(
        modifier = Modifier.width(140.dp),
        backgroundColor = Color(0xFFD4E3FF),
        borderColor = Color(0xFF6591E9),
        borderWidth = 8f,
        cornerSize = 8f
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = PixelFont,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun PlayerInfoDialog(
    player: WaitingPlayer,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            UserProfileCard(
                nickname = player.nickname,
                avatarUrl = player.profileUrl,

                // 🚧 [TODO] 나중에 실제 유저의 등급 데이터로 연결해야 합니다.
                policeGrade = "순경",
                thiefGrade = "바늘도둑",

                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun KickConfirmDialog(
    player: WaitingPlayer,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val reasons = listOf("욕설", "폭행", "비매너", "구역 이탈", "기타")
    var selectedReason by remember { mutableStateOf("욕설") }

    Dialog(onDismissRequest = onDismiss) {
        PixelContainer(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            backgroundColor = Color(0xFF2D3242),
            borderColor = Color(0xFF8D90B3),
            borderWidth = 4f,
            cornerSize = 16f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 타이틀
                Text(
                    text = "정말 강퇴하시겠습니까?",
                    fontFamily = PixelFont,
                    color = AccentYellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 대상 닉네임
                Text(
                    text = player.nickname,
                    fontFamily = PixelFont,
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 사유 선택 섹션
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "강퇴 사유",
                        fontFamily = PixelFont,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ReasonButtonRow(reasons.subList(0, 3), selectedReason) { selectedReason = it }
                    Spacer(modifier = Modifier.height(8.dp))
                    ReasonButtonRow(reasons.subList(3, 5), selectedReason) { selectedReason = it }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 강퇴하기 / 취소
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PixelIconButton(
                            onClick = { onConfirm(selectedReason) },
                            modifier = Modifier.fillMaxWidth(),
                            mainColor = Color.White,
                            borderColor = Color.Black,
                            pixelSize = 3.dp,
                            blockHeight = 12,
                            content = {
                                Text(
                                    text = "강퇴하기",
                                    fontFamily = PixelFont,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        PixelIconButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            mainColor = Color.White,
                            borderColor = Color.Black,
                            pixelSize = 3.dp,
                            blockHeight = 12,
                            content = {
                                Text(
                                    text = "취소",
                                    fontFamily = PixelFont,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReasonButtonRow(
    items: List<String>,
    selectedItem: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { reason ->
            val isSelected = (reason == selectedItem)
            val bgColor = if (isSelected) Color(0xFFD84315) else Color(0xFF37474F)
            val borderColor = if (isSelected) Color(0xFFFFCC80) else Color(0xFF78909C)

            Box(modifier = Modifier.weight(1f)) {
                PixelIconButton(
                    onClick = { onSelect(reason) },
                    modifier = Modifier.fillMaxWidth(),
                    mainColor = bgColor,
                    borderColor = borderColor,
                    pixelSize = 2.dp,
                    blockHeight = 10,
                    content = {
                        Text(
                            text = reason,
                            fontFamily = PixelFont,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }
        }

        if (items.size < 3) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}