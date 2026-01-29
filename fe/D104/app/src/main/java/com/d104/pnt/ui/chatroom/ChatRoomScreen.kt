package com.d104.pnt.ui.chatroom

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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.domain.model.ChatsData
import com.d104.pnt.ui.component.ChatList  // 🔥 추가!
import com.d104.pnt.ui.component.ChatRoomFooter
import com.d104.pnt.ui.component.ChatRoomHeader
import com.d104.pnt.ui.theme.DarkBackground

@Composable
fun ChatRoomScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    viewModel: ChatRoomViewModel = hiltViewModel()
) {
    // ViewModel에서 상태 가져오기
    val message by viewModel.message.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()

    val roomInfo by viewModel.roomInfo.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .imePadding()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            roomInfo?.let { info ->
                ChatRoomHeader(
                    modifier = Modifier.fillMaxWidth(),
                    roomData = ChatsData(
                        id = info.chatRoomId.toInt(),
                        title = info.title,
                        description = info.description,
                        maxMember = info.maxMembers,
                        currentMember = info.currentMembers
                    ),
                    onLeaveClick = { onBackPressed() }
                )
            }
        },
        bottomBar = {
            ChatRoomFooter(
                modifier = Modifier.fillMaxWidth(),
                onSendMessage = {
                    viewModel.sendMessage()
                },
                onValueChange = {
                    viewModel.writeMessage(it)
                },
                message = message
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ChatList(
                modifier = Modifier.fillMaxSize(),
                chatMessages = chatMessages,
                myMemberId = myMemberId,
                onSendMessage = { viewModel.sendMessage() },
                onScrollToBottom = {},
                onLoadMore = { viewModel.loadMoreMessages() }
            )
        }
    }
}
