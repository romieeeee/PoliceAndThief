package com.d104.pnt.ui.chatroom.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.d104.pnt.domain.model.ChatMessage
import com.d104.pnt.ui.chatroom.chat.ChatBubble
import com.d104.pnt.ui.theme.DarkBackground
import timber.log.Timber

@Composable
fun ChatList(
    modifier: Modifier,
    chatMessages: List<ChatMessage>,
    myMemberId: Long,
    onSendMessage: (String) -> Unit,
    onScrollToBottom: () -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    // 스크롤 맨 위에 도달했는지 감지
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index == chatMessages.size - 1 && chatMessages.isNotEmpty()
        }
    }

    // 맨 위 도달 시 이전 메시지 로드
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            Timber.d("📜 스크롤 맨 위 도달 - 이전 메시지 로드")
            onLoadMore()
        }
    }

    LazyColumn(
        modifier = modifier
            .background(DarkBackground)
            .fillMaxSize(),
        state = listState,
//        reverseLayout = true,
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
    }
}