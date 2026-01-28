package com.d104.pnt.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d104.pnt.domain.model.ChatsData

// 채팅방 리스트 컴포넌트
@Composable
fun RoomList(
    rooms: List<ChatsData>,
    onItemClick: (ChatsData) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp), // 리스트 전체 외곽 여백
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

// 디버깅용 프리뷰
@Preview(showBackground = true)
@Composable
fun RoomListPreview() {
    // 테스트용 더미 데이터 생성
    val dummyRooms = listOf(
        ChatsData(
            1,
            "진평동 빡겜 추격전",
            "날이 많이 추우니 장갑 꼭 챙겨오세요",
            30,
            25
        ),
        ChatsData(
            2,
            "강남역 상습 탈옥범 잡을 강력계형사 모집중",
            "뇌섹남녀 환영합니다. 초보 사절",
            25,
            17
        ),
    )

    RoomList(rooms = dummyRooms)
}