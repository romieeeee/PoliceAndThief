package com.d104.pnt.ui.game.wait

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.d104.pnt.R
import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.domain.model.DraggableLatLng
import com.d104.pnt.ui.component.GoogleMaps
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.RoundedButton
import com.d104.pnt.ui.game.create.CounterControl
import com.d104.pnt.ui.game.create.FactionRatioBar
import com.d104.pnt.ui.game.create.MapSettingDialog
import com.d104.pnt.ui.game.create.SectionTitle
import com.d104.pnt.ui.theme.DarkSurface
import com.d104.pnt.ui.theme.DialogBorderColor
import com.d104.pnt.ui.theme.PixelFont
import com.google.android.gms.maps.model.LatLng

@Composable
fun GameSettingsDialog(
    initialState: GameRoomInfoState,
    onDismiss: () -> Unit,
    onUpdateSettings: (Int, Int, Int, Int, Int, Location, List<Location>) -> Unit
) {
    var totalPlayers by remember { mutableStateOf(initialState.maxCount.coerceAtLeast(5)) }
    var gameTime by remember { mutableStateOf(initialState.timeLimit.coerceAtLeast(5)) }

    var missionCount by remember { mutableStateOf(initialState.missionCount) }
    var cctvCycle by remember { mutableStateOf(initialState.cctvCycle) }

    var policeCount by remember { mutableStateOf(initialState.policeCount) }
    val thiefCount = totalPlayers - policeCount

    val scrollState = rememberScrollState()

    var prisonLocation by remember { mutableStateOf(initialState.prison) }
    var polygonPoints by remember { mutableStateOf(initialState.polygon) }

    var showMapPopup by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        PixelContainer(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 700.dp),
            backgroundColor = DarkSurface,
            borderColor = DialogBorderColor,
            borderWidth = 8f,
            cornerSize = 16f
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "게임 설정 변경",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // --- 1. 맵 설정 & 감옥 설정 ---
                SectionTitle(text = "맵 설정 & 감옥 설정")
                Spacer(modifier = Modifier.height(8.dp))

                // 지도 표시 영역
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp) // 지도 높이도 살짝 조정 (공간 확보)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEEEEEE))
                ) {
                    GoogleMaps(
                        modifier = Modifier.fillMaxSize(),
                        inGameMinimap = false,
                        isPreview = true,
                        polygonPoints = polygonPoints?.map { DraggableLatLng(position = LatLng(it.lat, it.lng)) } ?: emptyList(),
                        prisonLocation = LatLng(prisonLocation?.lat ?: 37.56681969564895, prisonLocation?.lng ?: 126.97864094105321),
                    )

                    Box(
                        modifier = Modifier.fillMaxSize()
                            .clickable { showMapPopup = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "지도 수정하려면 클릭",
                            fontFamily = PixelFont,
                            color = Color.Black.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 2. 게임 규칙 ---
                SectionTitle(text = "게임 규칙")
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CounterControl(
                        Modifier.weight(1f), "플레이어", Icons.Default.Person,
                        totalPlayers.toString(), "명",
                        onDecrease = {
                            if (totalPlayers > 5) {
                                totalPlayers--
                                if (policeCount >= totalPlayers) {
                                    policeCount = totalPlayers - 1
                                }
                            }
                        },
                        onIncrease = {
                            if (totalPlayers < 30) {
                                totalPlayers++
                            }
                        }
                    )
                    CounterControl(
                        Modifier.weight(1f), "게임 시간", Icons.Default.Schedule,
                        gameTime.toString(), "분",
                        onDecrease = { if (gameTime > 5) gameTime -= 5 },
                        onIncrease = { if (gameTime < 60) gameTime += 5 }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CounterControl(
                        Modifier.weight(1f), "미션 갯수", Icons.Default.List,
                        missionCount.toString(), "개",
                        onDecrease = { if (missionCount > 5) missionCount-- },
                        onIncrease = { if (missionCount < 20) missionCount++ }
                    )
                    CounterControl(
                        Modifier.weight(1f), "CCTV 주기", Icons.Default.Videocam,
                        cctvCycle.toString(), "분",
                        onDecrease = { if (cctvCycle > 1) cctvCycle-- },
                        onIncrease = { if (cctvCycle < 20) cctvCycle++ }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. 진영 비율 ---
                SectionTitle(text = "진영 인원")
                Spacer(modifier = Modifier.height(8.dp))

                FactionRatioBar(
                    totalCount = totalPlayers,
                    policeCount = policeCount,
                    thiefCount = thiefCount,
                    onPoliceCountChange = { newPolice ->
                        if (newPolice in 1 until totalPlayers) {
                            policeCount = newPolice
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 취소 / 변경 완료
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RoundedButton(
                        text = "취소",
                        onClick = onDismiss,
                        containerColor = Color.White,
                        textColor = Color.Black,
                        modifier = Modifier.weight(1f)
                    )

                    RoundedButton(
                        text = "변경 완료",
                        onClick = {
                            onUpdateSettings(totalPlayers, gameTime, missionCount, cctvCycle, policeCount,
                                Location(prisonLocation!!.lat, prisonLocation!!.lng),
                                polygonPoints!!.map{ Location(it.lat, it.lng) }
                            )
                            onDismiss()
                        },
                        containerColor = Color.White,
                        textColor = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    if (showMapPopup) {
        GameWaitingMapSettingDialog(
            modifier = Modifier,
            onDismiss = { showMapPopup = false },
            onConfirm = { prison, polygon ->
                prisonLocation = prison
                polygonPoints = polygon
                showMapPopup = false
            }
        )
    }
}