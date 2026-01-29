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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.domain.model.ChatMessage
import com.d104.pnt.domain.model.ChatRoomData
import com.d104.pnt.ui.component.ChatList
import com.d104.pnt.ui.component.ChatRoomFooter
import com.d104.pnt.ui.component.ChatRoomHeader
import com.d104.pnt.ui.theme.DarkBackground

@Composable
fun ChatRoomScreen(
    modifier: Modifier,
    chatRoomData: ChatRoomData,
    chatMessages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onScrollToBottom: () -> Unit,
    viewModel: ChatRoomViewModel = hiltViewModel()
) {
    val message = viewModel.message.collectAsStateWithLifecycle()

    Scaffold (
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .imePadding()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            ChatRoomHeader(
                modifier = Modifier
                    .fillMaxWidth(),
                roomData = chatRoomData,
                onLeaveClick = {}
            )
        },
        bottomBar = {
            ChatRoomFooter(
                modifier = Modifier
                    .fillMaxWidth(),
                onSendMessage = onSendMessage,
                onValueChange = { viewModel.writeMessage(it) },
                message = message.value
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ChatList(
                modifier = Modifier
                    .fillMaxSize(),
                chatMessages = chatMessages,
                onSendMessage = onSendMessage,
                onScrollToBottom = onScrollToBottom,
            )
        }
    }
}