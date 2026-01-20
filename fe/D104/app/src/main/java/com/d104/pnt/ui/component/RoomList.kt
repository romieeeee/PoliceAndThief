package com.d104.pnt.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d104.pnt.domain.model.RoomData

// 채팅방 리스트 컴포넌트
@Composable
fun RoomList(
    rooms: List<RoomData>,
    onItemClick: (RoomData) -> Unit = {}
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp), // 리스트 전체 외곽 여백
        verticalArrangement = Arrangement.spacedBy(12.dp) // 아이템 사이 간격
    ) {
        items(rooms) { room ->
            RoomItem(
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
        RoomData(1, "진평동 빡겜 추격전", "날이 많이 추우니 장갑 꼭 챙겨오세요"),
        RoomData(2, "강남역 방탈출 고수만", "뇌섹남녀 환영합니다. 초보 사절"),
        RoomData(3, "한강공원 치맥 파티", "돗자리는 제가 가져갑니다 몸만 오세요"),
        RoomData(4, "개발자 모각코 구함", "노트북 필수, 콘센트 많은 카페로 갑니다"),
        RoomData(5, "새벽 롤 5인큐", "브론즈만 아니면 됩니다. 즐겜유저 환영")
    )

    RoomList(rooms = dummyRooms)
}