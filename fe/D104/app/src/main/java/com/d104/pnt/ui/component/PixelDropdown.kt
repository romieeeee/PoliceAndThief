package com.d104.pnt.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.d104.pnt.ui.theme.*

@Composable
fun PixelDropdown(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    label: String, // 1. 라벨을 외부에서 받도록 수정
    enabled: Boolean = true, // 2. 활성화 여부 추가 (기본값 true)
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val gapPx = with(density) { 4.dp.roundToPx() } // 버튼과 리스트 사이 4dp 간격
    var buttonHeight by remember { mutableIntStateOf(0) } // 드롭다운 박스 높이 (오프셋용)
    var buttonWidth by remember { mutableIntStateOf(0) } // 드롭다운 박스 너비 (오프셋용)

    Box(modifier = modifier
        .onGloballyPositioned { coordinates ->
            // 2. 컴포넌트가 배치될 때 높이와 너비를 측정해서 저장
            buttonWidth = coordinates.size.width
            buttonHeight = coordinates.size.height
        }
        .alpha(if (enabled) 1f else 0.3f)
    ) {
        // 1. 드롭다운 박스
        PixelContainer(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .clickable(enabled = enabled) { isExpanded = !isExpanded },
            backgroundColor = TextPrimary,
            borderColor = BorderDefault,
            cornerSize = 10f,   // 작은 모서리
            innerVerticalPadding = 15,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayText = if (selectedItem.isEmpty()) label else selectedItem
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelLarge,
                    color = BorderDefault

                )

                // 화살표 아이콘
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "드롭다운",
                    tint = BorderDefault,
                    modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                )
            }
        }

        // 2. 팝업 리스트 (열렸을 때만 보임)
        if (isExpanded && enabled) {
            // Popup을 사용하여 다른 UI 위에 띄움
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, buttonHeight + gapPx), // Y값을 버튼 높이만큼 내려야 함
                onDismissRequest = { isExpanded = false } // 바깥 클릭 시 닫힘
            ) {
                val popupWidthDp = with(density) { buttonWidth.toDp() }
                // 리스트 배경 디자인
                PixelContainer(
                    modifier = Modifier
                        .width(popupWidthDp) // 버튼 너비와 맞춤
                        .heightIn(max = 200.dp), // 드롭다운 리스트 높이
                    backgroundColor = TextPrimary,
                    borderColor = BorderDefault,
                    cornerSize = 10f,
                    innerHorizontalPadding = 4
                ) {
                    LazyColumn(
                        modifier = Modifier
                    ) {
                        itemsIndexed(items) { index, item ->
                            PixelDropdownItem(
                                text = item,
                                isSelected = item == selectedItem,
                                onClick = {
                                    onItemSelected(item)
                                    isExpanded = false // 선택 하면 닫기
                                }
                            )
                            if (index < items.lastIndex) { // 점선 구분선 배치
                                PixelDashedDivider(
                                    color = BorderDefault.copy(alpha = 0.5f), // 투명도 50%
                                    thickness = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}