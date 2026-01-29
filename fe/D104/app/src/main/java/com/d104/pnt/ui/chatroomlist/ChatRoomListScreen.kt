package com.d104.pnt.ui.chatroomlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelDropdown
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.component.RoomList
import com.d104.pnt.ui.theme.BorderDefault
import com.d104.pnt.ui.theme.ButtonHighlight
import com.d104.pnt.ui.theme.ButtonPrimary
import com.d104.pnt.ui.theme.DeepDark
import com.d104.pnt.ui.theme.TextPrimary

@Composable
fun ChatRoomListScreen(
    navigateToChatCreate: () -> Unit,
    viewModel: ChatRoomListViewModel = hiltViewModel()
) {
    val majors = viewModel.majorList
    val middles by viewModel.middleList.collectAsStateWithLifecycle()

    val selectedMajor by viewModel.selectedMajor.collectAsStateWithLifecycle()
    val selectedMiddle by viewModel.selectedMiddle.collectAsStateWithLifecycle()

    var searchMode by remember { mutableStateOf(false) }

    val uiState by viewModel.listState.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val searchText by viewModel.searchQuery.collectAsStateWithLifecycle()


    Scaffold(
        containerColor = DeepDark,
        floatingActionButton = {
            Box(
                modifier = Modifier.padding(end = 16.dp, bottom = 64.dp)
            ) {
                PixelIconButton(
                    modifier = Modifier
                        .size(56.dp)
                        .navigationBarsPadding(),
                    mainColor = ButtonPrimary,
                    borderColor = ButtonHighlight,
                    onClick = navigateToChatCreate
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "채팅방 만들기",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .statusBarsPadding() // 상태바 겹침 방지
        ) {
            // 1. 상단 버튼 영역 (Header)
            ChatRoomListHeader(
                majors = majors,
                middles = middles,
                viewMode = viewMode,
                selectedMajor = selectedMajor,
                selectedMiddle = selectedMiddle,
                onJoinedRoomClicked = {
                    viewModel.getJoinedChatRoom()
                },
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!searchMode) {
                    PixelIconButton(
                        modifier = Modifier.size(48.dp),
                        mainColor = ButtonPrimary,
                        borderColor = ButtonHighlight,
                        onClick = navigateToChatCreate
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "새 채팅방",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                }

                if (searchMode) {
                    PixelInputField(
                        modifier = Modifier
                            .weight(1f),
                        value = searchText,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = "검색어를 입력해주세요",
                        borderColor = BorderDefault,
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                PixelIconButton(
                    modifier = Modifier.size(48.dp),
                    mainColor = ButtonPrimary,
                    borderColor = ButtonHighlight,
                    onClick = {
                        val code =
                            if (viewModel.searchRegionQuery.value != -1) viewModel.searchRegionQuery.value else null
                        if (searchMode && searchText != "") {
                            viewModel.searchChatRoom(viewModel.searchQuery.value, code)
                        }
                        viewModel.updateSearchQuery("")
                        searchMode = !searchMode
                    }
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
            when (uiState) {
                is UiState.Idle -> {}
                is UiState.Loading -> {}
                is UiState.Success -> {
                    val data = (uiState as UiState.Success).data
                    if (data.chats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = "검색 결과가 없습니다.",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        Box(modifier = Modifier.weight(1f)) {
                            RoomList(
                                rooms = data.chats,
                                onItemClick = {}
                            )
                        }
                    }
                }

                is UiState.Error -> {}
            }
        }
    }
}

@Composable
fun ChatRoomListHeader(
    majors: List<String>,
    middles: List<String>,
    viewMode: ChatRoomListViewModel.ViewMode,
    selectedMajor: String,
    selectedMiddle: String,
    onJoinedRoomClicked: () -> Unit,
    onMajorSelected: (String) -> Unit,
    onMiddleSelected: (String) -> Unit
) {
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
            onClick = onJoinedRoomClicked,
            mainColor = if (viewMode == ChatRoomListViewModel.ViewMode.Me) ButtonPrimary else TextPrimary,
            pixelSize = 3.dp,
            blockHeight = 14,
            textColor = if (viewMode == ChatRoomListViewModel.ViewMode.Me) TextPrimary else BorderDefault
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 시/도 드롭다운
        PixelDropdown(
            items = majors,
            highlighted = viewMode == ChatRoomListViewModel.ViewMode.Region,
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
            highlighted = viewMode == ChatRoomListViewModel.ViewMode.Region,
            selectedItem = selectedMiddle,
            onItemSelected = onMiddleSelected,
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            label = "시|군|구"
        )
    }
}
