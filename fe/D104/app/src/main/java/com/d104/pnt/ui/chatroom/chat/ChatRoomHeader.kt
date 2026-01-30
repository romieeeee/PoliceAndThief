package com.d104.pnt.ui.chatroom.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.domain.model.ChatsData
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.theme.DarkBackground
import com.d104.pnt.ui.theme.DarkCard
import com.d104.pnt.ui.theme.TextPrimary
import com.d104.pnt.ui.theme.TextSecondary

@Composable
fun ChatRoomHeader(
    modifier: Modifier,
    roomData: ChatsData,
    onLeaveClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(DarkBackground)
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        PixelContainer(
            modifier = Modifier
                .fillMaxWidth(),
            cornerSize = 10f,
            backgroundColor = DarkCard,
            borderColor = TextSecondary,
            innerVerticalPadding = 16
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelContainer(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(onClick = { onLeaveClick() }),
                    backgroundColor = DarkCard,
                    borderColor = Color.White,
                    innerVerticalPadding = 10,
                    innerHorizontalPadding = 10
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            modifier = Modifier.size(32.dp),
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "나가기",
                            tint = Color.Unspecified,
                        )
                    }
                }

                Spacer(Modifier.width(20.dp))
                Text(
                    modifier = Modifier.weight(1f),
                    text = roomData.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${roomData.currentMember}/${roomData.maxMember}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )

                }
            }
        }
    }
}