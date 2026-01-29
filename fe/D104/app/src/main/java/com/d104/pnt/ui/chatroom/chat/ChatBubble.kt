package com.d104.pnt.ui.chatroom.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d104.pnt.domain.model.ChatMessage
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.theme.*
@Composable
fun ChatBubble(
    modifier: Modifier = Modifier,
    message: ChatMessage,
    isMe: Boolean,
){
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ){
        if (isMe){
            Spacer(modifier = Modifier.weight(0.3f))
        }
        Column(
            modifier = Modifier
                .weight(0.7f),
        ) {
            if (!isMe) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 0.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(TextSecondary) // 실제로는 이미지나 PixelContainer 축소판 사용
                            .padding(1.dp) // 테두리 느낌
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(TextDisabled))
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = message.senderNickname,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
            PixelContainer(
                modifier = Modifier,
                cornerSize = 30f,
                backgroundColor = if (isMe) ButtonHighlight else NeutralColor
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BorderDefault,
                )
            }
        }
        if (!isMe){
            Spacer(modifier = Modifier.weight(0.3f))
        }
    }
}

// 디버그용 프리뷰
@Preview
@Composable
fun PreviewChats(){
    Column () {
        ChatBubble(
            message = ChatMessage(
                id = 1,
                chatRoomId = 1,
                memberId = 1,
                senderNickname = "인동 대도",
                avataUrl = "",
                content = "2/5일 ㅇㅇ공원 근처에서 경도하실분!",
            ),
            isMe = false,
            modifier = Modifier
        )
        ChatBubble(
            message = ChatMessage(
                id = 2,
                chatRoomId = 1,
                memberId = 1,
                senderNickname = "런닝맨",
                avataUrl = "",
                content = "ㅇㅇ공원에서 할거고 경찰은 뿅망치 사용, 도둑은 빨간색 스티커 옷에 앞뒤로 붙이고 할 예정입니다",
            ),
            isMe = false,
            modifier = Modifier
        )
        ChatBubble(
            message = ChatMessage(
                id = 1,
                chatRoomId = 1,
                memberId = 1,
                senderNickname = "우사인 홈즈",
                avataUrl = "",
                content = "저 그날 시간 되요! 몇시에 할 예정인가요?",
            ),
            isMe = true,
            modifier = Modifier
        )
    }
}