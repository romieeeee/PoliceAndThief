package com.d104.pnt.ui.game.play

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.ui.component.ContDownUI
import com.d104.pnt.ui.component.ExpandableCard
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.game.play.PhoneScreen.THIEF_LIST
import com.d104.pnt.ui.theme.ButtonDisabled
import com.d104.pnt.ui.theme.MissionYellow

@Composable
fun GamePlayScreen(
    gameId: Long,
    role: GameRole,
    onGameEnd: () -> Unit
) {
    var clicked by remember { mutableStateOf(false) }
    var phoneScreen by remember { mutableStateOf(PhoneScreen.NO_SIGNAL) }

    Surface(modifier = Modifier.fillMaxSize()) {
        // 배경
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.bg_night),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        Box(modifier = Modifier.fillMaxSize()) {

            // 상단 버튼 영역 (고정)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .systemBarsPadding()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelIconButton(
                    modifier = Modifier.size(50.dp),
                    borderColor = ButtonDisabled,
                    pixelSize = 3.dp,
                    onClick = {
                        phoneScreen = PhoneScreen.MAP
                        clicked = !clicked
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_map),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }

                if (role == GameRole.POLICE) {
                    PixelIconButton(
                        modifier = Modifier.size(50.dp),
                        borderColor = ButtonDisabled,
                        pixelSize = 3.dp,
                        onClick = {
                            phoneScreen = PhoneScreen.CAMERA
                            clicked = !clicked
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                    }

                    PixelIconButton(
                        modifier = Modifier.size(50.dp),
                        borderColor = ButtonDisabled,
                        pixelSize = 3.dp,
                        onClick = {
                            phoneScreen = THIEF_LIST
                            clicked = !clicked
                        }
                    ) {
                        Icon(
                            painter = painterResource(if (role == GameRole.POLICE) R.drawable.ic_thief_list else R.drawable.ic_mission_list),
                            contentDescription = null,
                            tint = Color.Unspecified
                        )
                    }
                }


            }

            // 중앙 컨텐츠
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                ContDownUI(
                    remainingSeconds = 180
                )

                Spacer(modifier = Modifier.height(30.dp))

                if (role == GameRole.POLICE) {
                    FlipImage(
                        frontRes = role.badge,
                        backRes = R.drawable.img_helicopter // 뒷면 이미지
                    )
                } else {
                    Image(
                        modifier = Modifier.size(280.dp),
                        painter = painterResource(role.badge),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }

            }
        }

        if (role == GameRole.THIEF) {
            MissionBottomSheet {
                // == 안내 메시지 ==
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "미션 성공 시",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "CCTV에 포착되지 않습니다",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(20.dp))

                // LazyColumn이 자체적으로 스크롤됨
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    // 스크롤이 끝까지 가능하도록
                    userScrollEnabled = true
                ) {
                    items(6) { index ->
                        ExpandableCard(
                            title = "맨홀 뚜껑 촬영하기 ${index + 1}"
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "주변의 맨홀 뚜껑을 촬영하여 지하 탈출구를 확보하세요.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )

                                PixelContainer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = {}),
                                    backgroundColor = Color.Transparent,
                                    borderColor = MissionYellow,
                                    borderWidth = 8f
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        text = "미션 수행하기",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MissionYellow
                                    )
                                }
                            }
                        }
                    }

                    // 마지막 아이템 뒤 여백 추가
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }

        }

    }

    if (clicked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            PhoneFrame(
                phoneScreen,
                onScanSuccess = { result ->
                    phoneScreen = PhoneScreen.THIEF_LIST
//                    clicked = !clicked
                }
            )
        }
    }
}

enum class PhoneScreen {
    NO_SIGNAL,
    MAP,
    CAMERA,
    THIEF_LIST,
}
