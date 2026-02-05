package com.d104.pnt.ui.chatroom.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
    val density = LocalDensity.current

    // 새 메시지 올 때 자동 스크롤
    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    // 키보드 높이 감지
    val imeInsets = WindowInsets.ime
    val imeHeight = with(density) { imeInsets.getBottom(density).toDp() }

    // 키보드가 올라올 때 자동 스크롤
    LaunchedEffect(imeHeight) {
        if (imeHeight > 0.dp && chatMessages.isNotEmpty()) {
            // 키보드 열릴 때 맨 아래로 스크롤
            listState.scrollToItem(chatMessages.size - 1)
        }
    }


    // 🎯 상위 5개 메시지 중 하나라도 보이면 이전 메시지 로드
    LaunchedEffect(Unit) {
        snapshotFlow {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val firstVisibleIndex = visibleItems.firstOrNull()?.index ?: -1

            // 로딩 인디케이터 고려
            val threshold = if (isLoading && chatMessages.isNotEmpty()) {
                5  // 로딩 인디케이터 + 메시지 4개
            } else {
                4  // 메시지 4개
            }

            Triple(
                firstVisibleIndex in 0..threshold,  // 상위 5개 안에 있으면
                chatMessages.isNotEmpty(),
                isLoading
            )
        }
            .collect { (isNearTop, hasMessages, loading) ->
                if (isNearTop && hasMessages && !loading) {
                    Timber.d("🔝 상위 메시지 영역 도달 (index ≤ 4) - 이전 메시지 로드")
                    onLoadMore()
                }
            }
    }


    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            state = listState,
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
