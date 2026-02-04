package com.d104.pnt.ui.chatroom.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.d104.pnt.domain.model.ChatsData
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.ui.theme.DarkBackground
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import timber.log.Timber

@Composable
fun ChatRoomScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    viewModel: ChatRoomViewModel = hiltViewModel(),
    navController: NavController,
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val roomInfo by viewModel.roomInfo.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showKickedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Error -> {
                if (state.message == "채팅방에서 강퇴되었습니다") {
                    showKickedDialog = true
                }
            }
            else -> {}
        }
    }

    //  우측 드로어 상태 + 멤버 목록
    var drawerOpen by remember { mutableStateOf(false) }
    val members by viewModel.members.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            Timber.d("👋 사용자가 채팅방 화면을 떠납니다. 정리 시작!")
            // 주의: 여기서도 비동기 작업(API)을 하려면 ViewModel의 헬퍼 함수를 불러야 해
            viewModel.disconnectRoom()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            roomInfo?.let { info ->
                ChatRoomHeader(
                    modifier = Modifier.fillMaxWidth(),
                    roomData = ChatsData(
                        id = info.chatRoomId,
                        title = info.title,
                        description = info.description,
                        maxMember = info.maxMembers,
                        currentMember = info.currentMembers
                    ),
                    onLeaveClick = { onBackPressed() },
                    onMenuClick = {
                        drawerOpen = true
                        viewModel.loadMembers()
                    }
                )
            }
        },
        bottomBar = {
            ChatRoomFooter(
                modifier = Modifier.imePadding(),
                onSendMessage = { viewModel.sendMessage() },
                onValueChange = { viewModel.writeMessage(it) },
                message = message
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Chats(
                modifier = Modifier.fillMaxSize(),
                chatMessages = chatMessages,
                myMemberId = myMemberId,
                isLoading = isLoading,
                onLoadMore = { viewModel.loadMoreMessages() }
            )

            // 우측 멤버 드로어 오버레이
            ChatRoomMemberDrawer(
                visible = drawerOpen,
                members = members,
                myMemberId = myMemberId,
                onDismiss = { drawerOpen = false },
                onLeaveRoom = {
                    drawerOpen = false
                    viewModel.leaveRoom {
                        onBackPressed()
                    }
                },
                onDelegate = { targetId ->
                    viewModel.delegateHost(targetId)
                },
                onKick = { targetId, reason ->
                    viewModel.kickMember(targetId, reason)
                }
            )
        }
    }

    if (showKickedDialog) {
        KickedDialog(
            onDismiss = {
                showKickedDialog = false
                viewModel.clearUiState()
                navController.navigateUp()  // 메인 화면으로 이동
            }
        )
    }
}

@Composable
private fun KickedDialog(
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "강퇴 알림",
                style = MaterialTheme.typography.titleLarge,
                color = com.d104.pnt.ui.theme.AccentYellow
            )
        },
        text = {
            Text(
                text = "당신은 채팅방에서 강퇴되었습니다!",
                style = MaterialTheme.typography.bodyLarge,
                color = com.d104.pnt.ui.theme.TextPrimary
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(
                    text = "확인",
                    color = com.d104.pnt.ui.theme.AccentYellow
                )
            }
        },
        containerColor = com.d104.pnt.ui.theme.DarkCard
    )
}