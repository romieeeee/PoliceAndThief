package com.d104.pnt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.d104.pnt.domain.model.ChatMessage
import com.d104.pnt.ui.theme.DarkBackground

@Composable
fun ChatList(
    modifier: Modifier,
    chatMessages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onScrollToBottom: () -> Unit
){
    val myMemberId = 100
    LazyColumn (
        modifier = Modifier
            .background(DarkBackground)
            .fillMaxSize(),
        reverseLayout = true,
    ) {
        items (chatMessages) { chatMessage ->
            ChatBubble(
                message = chatMessage,
                isMe = if (chatMessage.memberId == myMemberId) true else false
            )
        }
    }
}

@Preview
@Composable
fun PreviewChatList(){
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
    )
    ChatList(
        modifier = Modifier,
        chatMessages = dummyMessages,
        onSendMessage = {},
        onScrollToBottom = {}
    )
}