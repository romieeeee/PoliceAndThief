package com.d104.pnt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d104.pnt.domain.model.ChatRoomData
import com.d104.pnt.ui.theme.DarkBackground
import com.d104.pnt.ui.theme.DarkCard
import com.d104.pnt.ui.theme.TextPrimary
import com.d104.pnt.ui.theme.TextSecondary

@Composable
fun ChatRoomHeader(
    modifier: Modifier,
    roomData: ChatRoomData,
    onLeaveClick: () -> Unit,
) {
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = roomData.title,
                    style = MaterialTheme.typography.titleMedium,
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
                    PixelIconButton(
                        onClick = onLeaveClick,
                        mainColor = DarkCard,
                        borderColor = TextPrimary,
                        pixelSize = 3.dp,
                        blockHeight = 10,
                        blockWidth = 10,
                    ) {
                        Icon(
                            modifier = Modifier.size(40.dp),
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "나가기",
                            tint = TextPrimary,
                        )
                    }
                }
            }
        }
    }
}