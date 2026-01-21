package com.d104.pnt.ui.game.create

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.theme.D104Theme
import com.d104.pnt.ui.theme.DarkSurface

private val DialogBorderColor = Color(0xFF81B0FF)

@Composable
fun GameCreateScreen() {
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PixelContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(650.dp),
                    backgroundColor = DarkSurface,
                    borderColor = DialogBorderColor,
                    borderWidth = 8f,
                    cornerSize = 16f
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 30.dp, vertical = 20.dp)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "게임 생성하기",
                            style = MaterialTheme.typography.titleLarge,
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
                                .height(140.dp)
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
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    PixelButtonCode(
                        text = "취소",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        mainColor = Color.White,
                        borderColor = Color.Black,
                        textColor = Color.Black,
                        fontSize = 16,
                        blockHeight = 13
                    )

                    PixelButtonCode(
                        text = "확인",
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        mainColor = Color.White,
                        borderColor = Color.Black,
                        textColor = Color.Black,
                        fontSize = 16,
                        blockHeight = 13
                    )
                }
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun CreateGameDialogPreview() {
//    D104Theme {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Gray),
//            contentAlignment = Alignment.Center
//        ) {
//            GameCreateScreen()
//        }
//    }
//}