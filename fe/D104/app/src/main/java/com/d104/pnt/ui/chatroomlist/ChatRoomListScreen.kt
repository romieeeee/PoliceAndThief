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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.domain.model.ChatsData
import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelDropdown
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.component.RoomList
import com.d104.pnt.ui.theme.*

@Composable
fun ChatRoomListScreen(
    navigateToChatCreate: () -> Unit,
    viewModel: ChatRoomListViewModel = hiltViewModel()
) {
    val majors = viewModel.majorList
    val middles by viewModel.middleList.collectAsStateWithLifecycle()

    val selectedMajor by viewModel.selectedMajor.collectAsStateWithLifecycle()
    val selectedMiddle by viewModel.selectedMiddle.collectAsStateWithLifecycle()
    // 테스트용 더미 데이터
    val roomList = List(10) {
        ChatsData(
            id = it,
            title = "진평동 빡겜 추격전",
            description = "날이 많이 추우니 장갑 꼭 챙겨오세요~~~~~~~~~~~~~~~~",
            maxMember = 30,
            currentMember = 26,
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
        ChatRoomListHeader(
            majors = majors,
            middles = middles,
            selectedMajor = selectedMajor,
            selectedMiddle = selectedMiddle,
            onMajorSelected = { newMajor ->
                viewModel.selectMajor(newMajor)
            },
            onMiddleSelected = { newMiddle ->
                viewModel.selectMiddle(newMiddle)
            }
        )

        Spacer(modifier = Modifier.height(5.dp))

        // 2. 검색 아이콘 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PixelIconButton (
                modifier = Modifier.size(48.dp),
                mainColor = ButtonPrimary,
                borderColor = ButtonHighlight,
                onClick = navigateToChatCreate
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "검색",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            PixelIconButton (
                modifier = Modifier.size(48.dp),
                mainColor = ButtonPrimary,
                borderColor = ButtonHighlight,
                onClick = { /* TODO: 검색 기능 */ }
            ) {
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
fun ChatRoomListHeader(
    majors: List<String>,
    middles: List<String>,
    selectedMajor: String,
    selectedMiddle: String,
    onMajorSelected: (String) -> Unit,
    onMiddleSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 12.dp),
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

        Spacer(modifier = Modifier.width(12.dp))

        // 시/도 드롭다운
        PixelDropdown(
            items = majors,
            selectedItem = selectedMajor,
            onItemSelected = onMajorSelected,
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            label = "시|도"
        )
        Spacer(modifier = Modifier.width(4.dp))
        // 시/군/구 드롭다운
        PixelDropdown(
            items = middles,
            selectedItem = selectedMiddle,
            onItemSelected = onMiddleSelected,
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            label = "시|군|구"
        )
    }
}