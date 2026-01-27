package com.d104.pnt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d104.pnt.ui.theme.ButtonPrimary
import com.d104.pnt.ui.theme.DarkBackground
import com.d104.pnt.ui.theme.DarkCard
import com.d104.pnt.ui.theme.TextPrimary
import com.d104.pnt.ui.theme.TextSecondary

@Composable
fun ChatRoomFooter(
    modifier: Modifier,
    message: String,
    onSendMessage: (String) -> Unit,
    onValueChange: (String) -> Unit
){
    Box(
        modifier = Modifier
            .background(DarkBackground)
            .fillMaxWidth()
    ) {
        PixelContainer(
            modifier = Modifier
                .fillMaxWidth(),
            cornerSize = 10f,
            backgroundColor = DarkCard,
            borderColor = TextSecondary,
            innerVerticalPadding = 5
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelInputField(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 5.dp),
                    placeholder = "채팅을 입력하세요",
                    backgroundColor = TextPrimary,
                    onValueChange = { onValueChange(it) },
                    value = message
                )
                PixelButtonCode(
                    modifier = Modifier,
                    text = "전송",
                    textColor = TextPrimary,
                    fontSize = 15,
                    onClick = {/* TODO: 채팅 전송 로직 작성 */ },
                    mainColor = ButtonPrimary,
                    borderColor = TextSecondary,
                    blockWidth = 15,
                    blockHeight = 10,
                )
            }
        }
    }
}