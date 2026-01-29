package com.d104.pnt.ui.game.create

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.R
import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.domain.model.DraggableLatLng
import com.d104.pnt.ui.component.GoogleMaps
//import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelInputField
import com.d104.pnt.ui.component.RoundedButton
import com.d104.pnt.ui.theme.DarkSurface
import com.d104.pnt.ui.theme.DialogBorderColor
import com.google.android.gms.maps.model.LatLng


@Composable
fun GameCreateScreen(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    viewModel: GameCreateViewModel = hiltViewModel()
) {
    val gameName by viewModel.gameName.collectAsStateWithLifecycle()
    val totalPlayers by viewModel.totalPlayers.collectAsStateWithLifecycle()
    val gameTime by viewModel.gameTime.collectAsStateWithLifecycle()
    val missionCount by viewModel.missionCount.collectAsStateWithLifecycle()
    val cctvCycle by viewModel.cctvCycle.collectAsStateWithLifecycle()
    val policeCount by viewModel.policeCount.collectAsStateWithLifecycle()
    val thiefCount by viewModel.thiefCount.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val polygonPoints by viewModel.polygonPoints.collectAsStateWithLifecycle()
    val prisonLocation by viewModel.prisonLocation.collectAsStateWithLifecycle()

    var showMapPopup by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // ViewModel에게 Context를 줘서 위치를 가져오고 저장하게 시킴
        viewModel.setDefaultSettings(context)
    }

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
                            borderColor = DialogBorderColor,
                            value = gameName,
                            onValueChange = { viewModel.updateGameName(it) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SectionTitle(text = "맵 설정 & 감옥 설정")
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            userLocation?.let { loc ->
                                GoogleMaps(
                                    modifier = Modifier.fillMaxSize(),
                                    inGameMinimap = false,
                                    isPreview = true,
                                    polygonPoints = polygonPoints.map { DraggableLatLng(position = it) },
                                    currentLocation = LatLng(
                                        viewModel.userLocation.value!!.latitude,
                                        viewModel.userLocation.value!!.longitude
                                    ),
                                    prisonLocation = prisonLocation,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { showMapPopup = true }
                            )// 여기서 클릭 감지 -> 팝업 띄우기
                        }

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
                                value = totalPlayers.toString(),
                                unit = "명",
                                onDecrease = { viewModel.updateTotalPlayers(false) },
                                onIncrease = { viewModel.updateTotalPlayers(true) }
                            )
                            CounterControl(
                                modifier = Modifier.weight(1f),
                                label = "게임 시간",
                                icon = Icons.Default.Schedule,
                                value = gameTime.toString(),
                                unit = "분",
                                onDecrease = { viewModel.updateGameTime(false) },
                                onIncrease = { viewModel.updateGameTime(true) }
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
                                value = missionCount.toString(),
                                unit = "개",
                                onDecrease = { viewModel.updateMissionCount(false) },
                                onIncrease = { viewModel.updateMissionCount(true) }
                            )
                            CounterControl(
                                modifier = Modifier.weight(1f),
                                label = "CCTV 주기",
                                icon = Icons.Default.Videocam,
                                value = cctvCycle.toString(),
                                unit = "분",
                                onDecrease = { viewModel.updateCctvCycle(false) },
                                onIncrease = { viewModel.updateCctvCycle(true) }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        SectionTitle(text = "진영 인원")
                        Spacer(modifier = Modifier.height(8.dp))

                        FactionRatioBar(
                            totalCount = totalPlayers,
                            policeCount = policeCount,
                            thiefCount = thiefCount,
                            onPoliceCountChange = { newCount -> viewModel.updatePoliceCount(newCount) }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            RoundedButton(
                                text = "취소",
                                onClick = {
                                    viewModel.dismissCreateGame()
                                    onCancel()
                                },
                                containerColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )

                            RoundedButton(
                                text = "확인",
                                onClick = {
                                    val polyPoint = polygonPoints.map { Location(lat = it.latitude, lng = it.longitude) }
                                    viewModel.createGameRoom(
                                        playerCount = totalPlayers,
                                        timeLimit = gameTime,
                                        policeCount = policeCount,
                                        thiefCount = thiefCount,
                                        prison = Location(lat = prisonLocation!!.latitude, lng = prisonLocation!!.longitude),
                                        polygon = polyPoint
                                    )
                                    onConfirm()
                                          },
                                containerColor = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
    if (showMapPopup) {
        MapSettingDialog (
            modifier = Modifier,
            onDismiss = { showMapPopup = false }, // 닫기 버튼이나 뒤로가기 시 닫힘
            onConfirm = { showMapPopup = false }
        )
    }
}

