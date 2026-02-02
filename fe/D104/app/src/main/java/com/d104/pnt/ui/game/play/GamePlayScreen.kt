package com.d104.pnt.ui.game.play

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.R
import com.d104.pnt.data.repository.GameSessionEvent
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.service.location.LocationService
import com.d104.pnt.ui.component.ContDownUI
import com.d104.pnt.ui.component.ExpandableCard
import com.d104.pnt.ui.component.GameEndOverlay
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.game.play.PhoneScreen.THIEF_LIST
import com.d104.pnt.ui.game.play.mission.MissionBottomSheet
import com.d104.pnt.ui.game.play.walkietalkie.WalkieBottomSheet
import com.d104.pnt.ui.game.play.walkietalkie.WalkieTalkieScreen
import com.d104.pnt.ui.theme.ButtonDisabled
import com.d104.pnt.ui.theme.MissionYellow
import com.d104.pnt.ui.theme.PixelFont
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay

@Composable
fun GamePlayScreen(
    gameId: Long,
    role: GameRole,
    onBackToHome: () -> Unit,
    onNavigateToLoading: (Long) -> Unit,
    goToCamera: () -> Unit,
    viewModel: GamePlayViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var backPressedTime by remember { mutableLongStateOf(0L) }

    var clicked by remember { mutableStateOf(false) }
    var phoneScreen by remember { mutableStateOf(PhoneScreen.NO_SIGNAL) }

    val currentLocation = viewModel.userLocation.collectAsState().value
    val areaPoints = viewModel.polygonPoints.collectAsStateWithLifecycle().value
    val prisonLocation = viewModel.prisonLocation.collectAsState().value

    val thiefMembers by viewModel.thiefMembers.collectAsStateWithLifecycle()

    val isOutOfBoundary by viewModel.isOutOfBoundary.collectAsStateWithLifecycle()

    var showGameOverOverlay by remember { mutableStateOf(false) }

    BackHandler {
        if (System.currentTimeMillis() - backPressedTime <= 1500) {
            viewModel.manualLeaveGame()
            onBackToHome()
        } else {
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(
                context,
                "한 번 더 누르면 게임에서 나갑니다",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 게임 초기화
    // TODO: 레포 기본값 채워주는 코드로 나중에는 지워야함
    LaunchedEffect(Unit) {
        viewModel.setDefaultArea(context)
        viewModel.initGame()

        viewModel.uiEvent.collect { event ->
            if (event is GameSessionEvent.NavigateToLoading) {
                // 2. 신호 오면 즉시 이동하지 말고 "게임 종료" 띄우기
                showGameOverOverlay = true

                // 3. 3초 동안 유저에게 보여줌 (이게 "샥" 하는 연출 시간)
                delay(3000L)

                // 4. 연출이 끝나면 그때서야 로딩 화면으로 이동
                onNavigateToLoading(event.gameId)
            }
        }
    }

    // 위치서비스 시작 / 종료
    DisposableEffect(Unit) {
        val serviceIntent = Intent(context, LocationService::class.java).apply {
            putExtra(LocationService.EXTRA_GAME_MODE, true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        onDispose {
            context.stopService(serviceIntent)
        }
    }


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

                FlipImage(
                    role = role,
                    memberId = 16L // TODO: 여기에 본인 멤버 아이디 넣기
                )

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

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                        .clickable(onClick = { goToCamera() }),
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

        } else {
            WalkieBottomSheet {
                WalkieTalkieScreen(
                    gameId = "1f",
                    teamType = "police"
                )

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
                screen = phoneScreen, // (변수명 screen으로 매칭)
                onScanSuccess = { result ->
                    phoneScreen = PhoneScreen.THIEF_LIST
                },
                role = role,
                currentLocation = LatLng(
                    currentLocation!!.latitude,
                    currentLocation!!.longitude
                ),
                areaPoints = areaPoints,
                prisonLocation = LatLng(
                    prisonLocation!!.latitude,
                    prisonLocation!!.longitude
                ),

                thiefMembers = thiefMembers
            )
        }
    }

// 경기구역이탈
    if (isOutOfBoundary) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(99f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.warning_overlay),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.fillMaxHeight(0.22f))

                Text(
                    text = "경기구역이탈!",
                    fontFamily = PixelFont,
                    color = Color.Red,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "경기 구역으로\n복귀하세요",
                    fontFamily = PixelFont,
                    color = Color.Red,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(130.dp))
            }
        }

    }

    AnimatedVisibility(
        visible = showGameOverOverlay,
        enter = slideInHorizontally() + fadeIn()
    ) {
        GameEndOverlay()
    }
}

enum class PhoneScreen {
    NO_SIGNAL,
    MAP,
    CAMERA,
    THIEF_LIST,
}
