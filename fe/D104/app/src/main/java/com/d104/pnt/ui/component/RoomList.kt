package com.d104.pnt.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d104.pnt.domain.model.ChatRoomData

// 채팅방 리스트 컴포넌트
@Composable
fun RoomList(
    rooms: List<ChatRoomData>,
    onItemClick: (ChatRoomData) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp), // 리스트 전체 외곽 여백
        verticalArrangement = Arrangement.spacedBy(12.dp) // 아이템 사이 간격
    ) {
        items(rooms) { room ->
            RoomListItem(
                data = room,
                onJoinClick = { onItemClick(room) }
            )
        }
    }
}