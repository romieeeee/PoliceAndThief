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

    // 새 메시지 올 때 맨 아래로 스크롤 (reverseLayout=true면 index 0이 맨 아래)
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(0)  // reverseLayout=true일 때 0이 맨 아래
        }
    }

    // 맨 위 도달 감지 (reverseLayout=true일 때 가장 오래된 메시지)
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index == chatMessages.size - 1 && chatMessages.isNotEmpty() && !isLoading
        }
    }

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
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
        ) {
            items(
                items = chatMessages,
                key = { it.id }
            ) { chatMessage ->
                ChatBubble(
                    message = chatMessage,
                    isMe = chatMessage.memberId == myMemberId
                )
            }

            // 로딩 인디케이터 (맨 위)
            if (isLoading && chatMessages.isNotEmpty()) {
                item(key = "loading_indicator") {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}