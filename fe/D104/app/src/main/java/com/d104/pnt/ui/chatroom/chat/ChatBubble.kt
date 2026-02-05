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
import com.d104.pnt.ui.theme.BorderDefault
import com.d104.pnt.ui.theme.ButtonHighlight
import com.d104.pnt.ui.theme.DarkBackground
import com.d104.pnt.ui.theme.NeutralColor
import com.d104.pnt.ui.theme.TextDisabled
import com.d104.pnt.ui.theme.TextPrimary
import com.d104.pnt.ui.theme.TextSecondary
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.d104.pnt.R

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
            Box(
                modifier = Modifier
                    .size(40.dp)
//                    .clip(CircleShape) // 원형으로 자르기
                    .background(TextSecondary)
            ) {
                AsyncImage(
                    model = if (message.avatarUrl.isNullOrEmpty() || message.avatarUrl == "string") {
                        R.drawable.profile_img_default // URL이 없거나 "string"이면 기본 이미지
                    } else {
                        // 만약 상대경로라면 앞부분 붙여주기
                        if (message.avatarUrl.startsWith("http")) message.avatarUrl
                        else "https://i14d104.p.ssafy.io/spring/${message.avatarUrl}"
                    },
                    contentDescription = "프로필 이미지",
                    placeholder = painterResource(R.drawable.profile_img_default),
                    error = painterResource(R.drawable.profile_img_default),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
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
