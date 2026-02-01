package com.d104.pnt.ui.chatroom.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.theme.DarkBackground
import com.d104.pnt.ui.theme.DarkCard
import com.d104.pnt.ui.theme.TextPrimary
import com.d104.pnt.ui.theme.TextSecondary

@Composable
fun ChatRoomMemberDrawer(
    visible: Boolean,
    members: List<ChatRoomMemberUi>,
    onDismiss: () -> Unit,
    onLeaveRoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 스크림(바깥 클릭 시 닫기)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(160)),
        exit = fadeOut(animationSpec = tween(160)),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable { onDismiss() }
        )
    }

    // 우측 패널
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            animationSpec = tween(220),
            initialOffsetX = { fullWidth -> fullWidth }
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(220),
            targetOffsetX = { fullWidth -> fullWidth }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            PixelContainer(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp, horizontal = 12.dp)
                    // 패널 내부 클릭이 바깥 dismiss로 전파되는 거 방지
                    .clickable(enabled = false) {},
                cornerSize = 10f,
                backgroundColor = DarkCard,
                borderColor = TextSecondary,
                innerVerticalPadding = 16,
                innerHorizontalPadding = 16
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // 상단: 멤버 목록
                    Column {
                        Text(
                            text = "멤버 목록",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        Spacer(Modifier.height(12.dp))
                        Divider(color = TextSecondary)
                        Spacer(Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(members, key = { it.memberId }) { m ->
                                MemberRow(m)
                            }
                        }
                    }

                    // 하단: 나가기 버튼
                    Column {
                        Spacer(Modifier.height(12.dp))
                        Divider(color = TextSecondary)
                        Spacer(Modifier.height(12.dp))

                        PixelContainer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { onLeaveRoom() },
                            backgroundColor = DarkBackground,
                            borderColor = TextSecondary,
                            innerVerticalPadding = 12,
                            innerHorizontalPadding = 12
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "채팅방 나가기",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: ChatRoomMemberUi) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = member.nickname,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
        Text(
            text = if (member.isHost) "방장" else "멤버",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

data class ChatRoomMemberUi(
    val memberId: Long,
    val nickname: String,
    val isHost: Boolean = false
)
