package com.d104.pnt.ui.chatroom.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.theme.ButtonPrimary
import com.d104.pnt.ui.theme.DarkBackground
import com.d104.pnt.ui.theme.TextPrimary
import com.d104.pnt.ui.theme.TextSecondary

@Composable
fun ChatRoomFooter(
    message: String,
    onSendMessage: (String) -> Unit,
    onValueChange: (String) -> Unit
) {
    // 메시지가 비어있는지 확인 (버튼 활성화 여부)
    val isMessageEmpty = message.isBlank()

    Box(
        modifier = Modifier
            .background(DarkBackground)
            .fillMaxWidth()
            .padding(top = 1.dp),
        contentAlignment = Alignment.Center
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically // 중앙 정렬로 변경
        ) {
            PixelInputField(
                modifier = Modifier
                    .weight(1f),
                placeholder = "메시지를 입력하세요...",
                backgroundColor = TextPrimary,
                value = message,
                onValueChange = onValueChange
            )

            PixelContainer(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(enabled = !isMessageEmpty) {
                        onSendMessage(message)
                    },
                backgroundColor = if (isMessageEmpty) TextSecondary else ButtonPrimary,
                borderColor = if (isMessageEmpty) TextSecondary else ButtonPrimary,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send),
                        contentDescription = "전송",
                        modifier = Modifier.size(24.dp),
                        // 비활성 상태일 때 투명도 조절
                        tint = if (isMessageEmpty) Color.Gray else Color.Unspecified
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun FooterPreview() {
    ChatRoomFooter(
        "", { }, {}
    )
}