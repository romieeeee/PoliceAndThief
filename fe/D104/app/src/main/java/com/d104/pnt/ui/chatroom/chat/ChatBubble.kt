package com.d104.pnt.ui.chatroom.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        // 내 메시지가 아닐 때만 프로필 + 닉네임 표시
        if (!isMe) {
            // 프로필 이미지
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(TextSecondary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(TextDisabled)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 260.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            // 상대방 메시지일 때만 닉네임 표시
            if (!isMe) {
                Text(
                    text = message.senderNickname,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            // 말풍선
            PixelContainer(
                modifier = Modifier,
                cornerSize = 20f,
                backgroundColor = if (isMe) ButtonHighlight else NeutralColor
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) TextPrimary else BorderDefault,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

// 디버그용 프리뷰
@Preview(showBackground = true, backgroundColor = 0xFF1A1A2E)
@Composable
fun PreviewChats() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChatBubble(
            message = ChatMessage(
                id = 1,
                chatRoomId = 1,
                memberId = 1,
                senderNickname = "인동 대도",
                avataUrl = "",
                content = "2/5일 ㅇㅇ공원 근처에서 경도하실분!",
            ),
            isMe = false
        )
        ChatBubble(
            message = ChatMessage(
                id = 2,
                chatRoomId = 1,
                memberId = 2,
                senderNickname = "런닝맨",
                avataUrl = "",
                content = "ㅇㅇ공원에서 할거고 경찰은 뿅망치 사용, 도둑은 빨간색 스티커 옷에 앞뒤로 붙이고 할 예정입니다",
            ),
            isMe = false
        )
        ChatBubble(
            message = ChatMessage(
                id = 3,
                chatRoomId = 1,
                memberId = 3,
                senderNickname = "우사인 홈즈",
                avataUrl = "",
                content = "저 그날 시간 돼요! 몇시에 할 예정인가요?",
            ),
            isMe = true
        )
        ChatBubble(
            message = ChatMessage(
                id = 4,
                chatRoomId = 1,
                memberId = 1,
                senderNickname = "인동 대도",
                avataUrl = "",
                content = "오후 3시!",
            ),
            isMe = false
        )
    }
}