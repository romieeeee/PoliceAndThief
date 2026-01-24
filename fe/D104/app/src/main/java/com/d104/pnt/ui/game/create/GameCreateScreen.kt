package com.d104.pnt.ui.game.create

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.component.RoundedButton
import com.d104.pnt.ui.theme.DarkSurface
import com.d104.pnt.ui.theme.DialogBorderColor


@Composable
fun GameCreateScreen(
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    var gameName by remember { mutableStateOf("") }
    var totalPlayers by remember { mutableStateOf(25) }
    var gameTime by remember { mutableStateOf(20) }
    var missionCount by remember { mutableStateOf(5) }
    var cctvCycle by remember { mutableStateOf(10) }
    var policeCount by remember { mutableStateOf(9) }

    val thiefCount = totalPlayers - policeCount

    Surface(modifier = Modifier.fillMaxSize()) {

        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.img_main_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 게임 생성 컨테이너
                PixelContainer(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DarkSurface,
                    borderColor = DialogBorderColor,
                    borderWidth = 8f,
                    cornerSize = 16f
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "게임 생성하기",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        SectionTitle(text = "게임 이름 설정")
                        Spacer(modifier = Modifier.height(8.dp))
                        PixelInputField(
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "방 이름을 입력하세요",
                            borderColor = DialogBorderColor
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SectionTitle(text = "맵 설정 & 감옥 설정")
                        Spacer(modifier = Modifier.height(8.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_map_example),
                            contentDescription = "맵 프리뷰",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(120.dp)
                                .background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SectionTitle(text = "게임 규칙")
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CounterControl(
                                modifier = Modifier.weight(1f),
                                label = "플레이어",
                                icon = Icons.Default.Person,
                                value = "$totalPlayers",
                                unit = "명",
                                onDecrease = {
                                    if (totalPlayers > 5) {
                                        totalPlayers--
                                        if (policeCount >= totalPlayers) {
                                            policeCount = totalPlayers - 1
                                        }
                                    }
                                },
                                onIncrease = { if (totalPlayers < 30) totalPlayers++ }
                            )
                            CounterControl(
                                modifier = Modifier.weight(1f),
                                label = "게임 시간",
                                icon = Icons.Default.Schedule,
                                value = "$gameTime",
                                unit = "분",
                                onDecrease = { if (gameTime > 10) gameTime -= 5 },
                                onIncrease = { if (gameTime < 60) gameTime += 5 }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CounterControl(
                                modifier = Modifier.weight(1f),
                                label = "미션 갯수",
                                icon = Icons.Default.List,
                                value = "$missionCount",
                                unit = "개",
                                onDecrease = { if (missionCount > 1) missionCount-- },
                                onIncrease = { if (missionCount < 20) missionCount++ }
                            )
                            CounterControl(
                                modifier = Modifier.weight(1f),
                                label = "CCTV 주기",
                                icon = Icons.Default.Videocam,
                                value = "$cctvCycle",
                                unit = "분",
                                onDecrease = { if (cctvCycle > 1) cctvCycle-- },
                                onIncrease = { if (cctvCycle < 30) cctvCycle++ }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        SectionTitle(text = "진영 인원")
                        Spacer(modifier = Modifier.height(8.dp))

                        FactionRatioBar(
                            totalCount = totalPlayers,
                            policeCount = policeCount,
                            thiefCount = thiefCount,
                            onPoliceCountChange = { newCount -> policeCount = newCount }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            RoundedButton(
                                text = "취소",
                                onClick = { onCancel() },
                                containerColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            RoundedButton(
                                text = "확인",
                                onClick = { onConfirm() },
                                containerColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

