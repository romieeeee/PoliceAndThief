package com.d104.pnt.ui.game.wait

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.R
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.theme.PixelFont

@Composable
fun GameWaitingScreen(
    roomId: Long,
    initialRole: GameRole,
    onChangeRole: () -> Unit,
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

    // 이벤트 처리
    LaunchedEffect(Unit) {
        viewModel.setInitialRole(initialRole)
        viewModel.uiEvent.collect { event ->
            when (event) {
                is GameWaitingUiEvent.NavigateToHome -> {
                    if (!onBackPressed()) onNavigateHome(event.message)
                }
            }
        }
    }

    BackHandler {
        viewModel.leaveRoom()
    }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) onStartGame(roomId, GameRole.POLICE)
    }

    val policeCount = players.count { it.role == GameRole.POLICE && !it.isChangingRole }
    val thiefCount = players.count { it.role == GameRole.THIEF && !it.isChangingRole }

    val anyCount = players.size - policeCount - thiefCount

    val isAllReady = players.isNotEmpty() && players.filter { it.id != myMemberId }.all {
        it.isReady && !it.isChangingRole && it.role != GameRole.ANY
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

            // 방 정보
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

            // 보드
            UnifiedWaitingInfoCard(
                players = players,
                myMemberId = myMemberId,
                policeCount = policeCount,
                thiefCount = thiefCount,
                anyCount = anyCount,
                isHost = isHost,
                selectedPlayerId = selectedPlayerId,
                onPlayerClick = { player ->
                    val now = System.currentTimeMillis()
                    if (!((dismissedPlayerId == player.id) && (now - lastDismissTime < 300))) selectedPlayerId =
                        if (selectedPlayerId == player.id) null else player.id
                },
                onMenuDismiss = {
                    dismissedPlayerId = selectedPlayerId
                    lastDismissTime = System.currentTimeMillis()
                    selectedPlayerId = null
                },
                onInfoClick = { player ->
                    dismissedPlayerId = selectedPlayerId
                    lastDismissTime = System.currentTimeMillis()
                    selectedPlayerId = null
                    infoDialogTarget = player
                },
                onKickClick = { player ->
                    dismissedPlayerId = selectedPlayerId; lastDismissTime =
                    System.currentTimeMillis(); selectedPlayerId = null; kickDialogTarget =
                    player
                },
                onChangeRole = {
                    viewModel.resetToUndecided()
                    onChangeRole()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 준비 / 시작
            val buttonText = if (isHost) "게임 시작" else if (isMeReady) "준비 취소" else "준비"
            val buttonColor = if (isHost) {
                if (isAllReady) Color.White else Color.Gray
            } else {
                if (isMeReady) Color.Gray else Color.White
            }
            val textColor = if (buttonColor == Color.Gray) Color.White else Color.Black

            PixelIconButton(
                onClick = {
                    if (isHost) {
                        if (isAllReady) viewModel.startGame()
                    } else viewModel.toggleReady()
                },
                modifier = Modifier.fillMaxWidth(),
                mainColor = buttonColor,
                borderColor = if (buttonColor == Color.Gray) Color.DarkGray else Color.Black,
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
            Spacer(modifier = Modifier.height(60.dp))
        }

        // ========== 다이얼로그 ==========
        if (infoDialogTarget != null) PlayerInfoDialog(
            player = infoDialogTarget!!,
            onDismiss = { infoDialogTarget = null })

        if (kickDialogTarget != null) KickConfirmDialog(
            player = kickDialogTarget!!,
            onDismiss = { kickDialogTarget = null },
            onConfirm = { reason ->
                viewModel.kickPlayer(
                    kickDialogTarget!!.id,
                    reason
                ); kickDialogTarget = null
            })

        if (showSettingsDialog) GameSettingsDialog(
            initialState = roomInfo,
            onDismiss = { showSettingsDialog = false },
            onUpdateSettings = { total, time, mission, cctv, police ->
                viewModel.updateRoomSettings(
                    total,
                    time,
                    mission,
                    cctv,
                    police
                )
            })
    }
}