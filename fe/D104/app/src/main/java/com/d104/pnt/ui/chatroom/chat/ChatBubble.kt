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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d104.pnt.domain.model.ChatMessage
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.theme.BorderDefault
import com.d104.pnt.ui.theme.ButtonHighlight
import com.d104.pnt.ui.theme.DarkBackground
import com.d104.pnt.ui.theme.NeutralColor
import com.d104.pnt.ui.theme.TextDisabled
import com.d104.pnt.ui.theme.TextPrimary
import com.d104.pnt.ui.theme.TextSecondary

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
                backgroundColor = Color.White.copy(alpha = 0.8f)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}
