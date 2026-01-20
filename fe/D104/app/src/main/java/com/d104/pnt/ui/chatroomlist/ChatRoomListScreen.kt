package com.d104.pnt.ui.chatroomlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d104.pnt.domain.model.RoomData
import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelDropdown
import com.d104.pnt.ui.component.RoomList
import com.d104.pnt.ui.theme.*

@Composable
fun ChatRoomListScreen() {
    // 테스트용 더미 데이터
    val roomList = List(10) {
        RoomData(
            id = it,
            title = "진평동 빡겜 추격전",
            description = "날이 많이 추우니 장갑 꼭 챙겨오세요~~~~~~~~~~~~~~~~",
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark)
            .padding(16.dp)
            .statusBarsPadding() // 상태바 겹침 방지
    ) {
        // 1. 상단 버튼 영역 (Header)
        ChatRoomListHeader()

        Spacer(modifier = Modifier.height(5.dp))

        // 2. 검색 아이콘 영역
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd // 오른쪽 정렬
        ) {
            IconButton(onClick = { /* TODO: 검색 기능 */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "검색",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // 3. 리스트 영역
        Box(modifier = Modifier.weight(1f)) {
            RoomList(
                rooms = roomList,
                onItemClick = {}
            )
        }
    }
}

@Composable
fun ChatRoomListHeader() {
    val regionList = listOf("서울", "인천", "대구", "부산", "대전", "광주", "울산", "세종")
    var selectedRegion by remember { mutableStateOf(regionList[0]) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 왼쪽 버튼
        PixelButtonCode(
            modifier = Modifier.weight(1f),
            text = "참여 중인 채팅방",
            fontSize = 14,
            onClick = { },
            mainColor = ButtonPrimary,
            pixelSize = 3.dp,
            blockHeight = 14,
            textColor = TextPrimary
        )

        // 오른쪽 드롭다운
        PixelDropdown(
            items = regionList,
            selectedItem = selectedRegion,
            onItemSelected = { selectedRegion = it },
            modifier = Modifier.weight(1f)
        )
    }
}

// 디버깅용 미리보기
@Preview
@Composable
fun ChatRoomScreenPreview() {
    ChatRoomListScreen()
}