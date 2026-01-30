package com.d104.pnt.ui.chatroom.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d104.pnt.domain.model.ChatMessage
import com.d104.pnt.ui.theme.DarkBackground
import timber.log.Timber

@Composable
fun Chats(
    modifier: Modifier = Modifier,
    chatMessages: List<ChatMessage>,
    myMemberId: Long,
    isLoading: Boolean = false,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    // 새 메시지 올 때 자동 스크롤 (맨 아래로)
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // 스크롤 맨 위에 도달했는지 감지 (이전 메시지 로드용)
    val shouldLoadMore by remember {
        derivedStateOf {
            val firstVisibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            firstVisibleItem?.index == 0 && chatMessages.isNotEmpty() && !isLoading
        }
    }

    // 맨 위 도달 시 이전 메시지 로드
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            Timber.d("스크롤 맨 위 도달 - 이전 메시지 로드")
            onLoadMore()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            state = listState,
            reverseLayout = false,
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 로딩 인디케이터 (맨 위)
            if (isLoading && chatMessages.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // 메시지 리스트
            items(
                items = chatMessages,
                key = { it.id }
            ) { chatMessage ->
                ChatBubble(
                    message = chatMessage,
                    isMe = chatMessage.memberId == myMemberId
                )
            }
        }
    }
}