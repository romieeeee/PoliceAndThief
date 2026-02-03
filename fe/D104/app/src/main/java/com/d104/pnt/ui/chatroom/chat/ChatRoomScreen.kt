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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.domain.model.ChatsData
import com.d104.pnt.ui.theme.DarkBackground

@Composable
fun ChatRoomScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    viewModel: ChatRoomViewModel = hiltViewModel()
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val roomInfo by viewModel.roomInfo.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    //  우측 드로어 상태 + 멤버 목록
    var drawerOpen by remember { mutableStateOf(false) }
    val members by viewModel.members.collectAsStateWithLifecycle()

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
                myMemberId = myMemberId, // ✅ ID 전달
                onDismiss = { drawerOpen = false },
                onLeaveRoom = {
                    drawerOpen = false
                    viewModel.leaveRoom {
                        onBackPressed()
                    }
                },
                onDelegate = { targetId -> // ✅ 위임 액션 연결
                    viewModel.delegateHost(targetId)
                },
                onKick = { targetId, reason -> // ✅ 강퇴 액션 연결
                    viewModel.kickMember(targetId, reason)
                }
            )

        }
    }
}
