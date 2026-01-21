package com.d104.pnt.ui.chatroom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.d104.pnt.domain.model.ChatMessage
import com.d104.pnt.domain.model.RoomData
import com.d104.pnt.ui.component.ChatList
import com.d104.pnt.ui.component.ChatRoomFooter
import com.d104.pnt.ui.component.ChatRoomHeader
import com.d104.pnt.ui.theme.DarkBackground

@Composable
fun ChatRoomScreen(
    modifier: Modifier,
    chatRoomData: RoomData,
    chatMessages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onScrollToBottom: () -> Unit
) {
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
                onSendMessage = onSendMessage
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

@Preview(heightDp = 800)
@Composable
fun PreviewChatRoomScreen(){
    val dummyMessages = listOf(
        ChatMessage(
            id = 3,
            chatRoomId = 1,
            memberId = 100,
            senderNickname = "우사인 홈즈",
            avataUrl = "",
            content = "저 그날 시간 되요! 몇시에 할 예정인가요?",
        ),
        ChatMessage(
            id = 2,
            chatRoomId = 1,
            memberId = 2,
            senderNickname = "런닝맨",
            avataUrl = "",
            content = "ㅇㅇ공원에서 할거고 경찰은 뿅망치 사용, 도둑은 빨간색 스티커 옷에 앞뒤로 붙이고 할 예정입니다",
        ),
        ChatMessage(
            id = 1,
            chatRoomId = 1,
            memberId = 1,
            senderNickname = "인동 대도",
            avataUrl = "",
            content = "2/5일 ㅇㅇ공원 근처에서 경도하실분!",
        ),
        ChatMessage(
            id = 3,
            chatRoomId = 1,
            memberId = 100,
            senderNickname = "우사인 홈즈",
            avataUrl = "",
            content = "저 그날 시간 되요! 몇시에 할 예정인가요?",
        ),
        ChatMessage(
            id = 2,
            chatRoomId = 1,
            memberId = 2,
            senderNickname = "런닝맨",
            avataUrl = "",
            content = "ㅇㅇ공원에서 할거고 경찰은 뿅망치 사용, 도둑은 빨간색 스티커 옷에 앞뒤로 붙이고 할 예정입니다",
        ),
        ChatMessage(
            id = 1,
            chatRoomId = 1,
            memberId = 1,
            senderNickname = "인동 대도",
            avataUrl = "",
            content = "2/5일 ㅇㅇ공원 근처에서 경도하실분!",
        ),
        ChatMessage(
            id = 3,
            chatRoomId = 1,
            memberId = 100,
            senderNickname = "우사인 홈즈",
            avataUrl = "",
            content = "저 그날 시간 되요! 몇시에 할 예정인가요?",
        ),
        ChatMessage(
            id = 2,
            chatRoomId = 1,
            memberId = 2,
            senderNickname = "런닝맨",
            avataUrl = "",
            content = "ㅇㅇ공원에서 할거고 경찰은 뿅망치 사용, 도둑은 빨간색 스티커 옷에 앞뒤로 붙이고 할 예정입니다",
        ),
        ChatMessage(
            id = 1,
            chatRoomId = 1,
            memberId = 1,
            senderNickname = "인동 대도",
            avataUrl = "",
            content = "2/5일 ㅇㅇ공원 근처에서 경도하실분!",
        ),
    )
    ChatRoomScreen(
        modifier = Modifier,
        chatRoomData = RoomData(
            1,
            "진평동 빡겜 추격전 진평동 빡겜 추격전 진평동 빡겜 추격전 진평동 빡겜 추격전",
            "날이 많이 추우니 장갑 꼭 챙겨오세요",
            30,
            25
        ),
        onSendMessage = {},
        chatMessages = dummyMessages,
        onScrollToBottom = {}
    )
}