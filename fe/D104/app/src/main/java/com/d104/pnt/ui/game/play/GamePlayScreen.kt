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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.d104.pnt.data.repository.ArrestStatus
import com.d104.pnt.data.repository.CctvPhase
import com.d104.pnt.data.repository.GameSessionEvent
import com.d104.pnt.data.repository.HelicopterPhase
import com.d104.pnt.data.repository.MissionStatus
import com.d104.pnt.data.repository.WalkieConnectionState
import com.d104.pnt.data.repository.WarningReason
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.domain.model.Mission
import com.d104.pnt.service.location.LocationService
import com.d104.pnt.ui.component.AlertOverlay
import com.d104.pnt.ui.component.ContDownUI
import com.d104.pnt.ui.component.ExpandableCard
import com.d104.pnt.ui.component.GameEndOverlay
import com.d104.pnt.ui.component.PixelAlertDialog
import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.component.PixelIconButton
import com.d104.pnt.ui.component.PixelLoading
import com.d104.pnt.ui.component.WarningOverlay
import com.d104.pnt.ui.game.play.PhoneScreen.THIEF_LIST
import com.d104.pnt.ui.game.play.mission.MissionBottomSheet
import com.d104.pnt.ui.game.play.walkietalkie.WalkieBottomSheet
import com.d104.pnt.ui.game.play.walkietalkie.WalkieTalkieContent
import com.d104.pnt.ui.theme.ButtonDisabled
import com.d104.pnt.ui.theme.LoseColor
import com.d104.pnt.ui.theme.MissionYellow
import com.d104.pnt.ui.theme.PixelFont
import com.d104.pnt.ui.theme.WinColor
import com.d104.pnt.util.GameFeedbackManager
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun GamePlayScreen(
    gameId: Long,
    role: GameRole,
    onBackToHome: () -> Unit,
    onNavigateToLoading: (Long) -> Unit,
    goToCamera: (Long) -> Unit,
    viewModel: GamePlayViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var backPressedTime by remember { mutableLongStateOf(0L) }
    var clicked by remember { mutableStateOf(false) }
    var phoneScreen by remember { mutableStateOf(PhoneScreen.NO_SIGNAL) }
    var showGameOverOverlay by remember { mutableStateOf(false) }
    var showThiefEscaped by remember { mutableStateOf(false) }
    var escapedThiefNickname by remember { mutableStateOf("") }
    val feedbackManager = remember { GameFeedbackManager(context.applicationContext) }

    val currentLocation = viewModel.userLocation.collectAsStateWithLifecycle().value
    val areaPoints = viewModel.polygonPoints.collectAsStateWithLifecycle().value
    val prisonLocation = viewModel.prisonLocation.collectAsStateWithLifecycle().value
    val warningReason = viewModel.warningStatus.collectAsStateWithLifecycle()
    val thiefMembers by viewModel.thiefMembers.collectAsStateWithLifecycle()
    val memberLocation by viewModel.memberLocation.collectAsStateWithLifecycle()
    val escapeQueue by viewModel.escapeQueue.collectAsStateWithLifecycle()
    val missions by viewModel.missions.collectAsStateWithLifecycle()
    val missionState by viewModel.missionState.collectAsStateWithLifecycle()
    val missionFailReason by viewModel.missionFailReason.collectAsStateWithLifecycle()
    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()
    val helicopterState by viewModel.helicopterState.collectAsStateWithLifecycle()
    val isChief by viewModel.isChief.collectAsStateWithLifecycle()
    val helicopterEnabled by viewModel.helicopterButtonEnabled.collectAsStateWithLifecycle()
    val walkieState by viewModel.walkieState.collectAsStateWithLifecycle()
    val walkieConnected by viewModel.walkieConnected.collectAsStateWithLifecycle()
    val walkieMicEnabled by viewModel.walkieMicEnabled.collectAsStateWithLifecycle()
    val walkieParticipantCount by viewModel.walkieParticipantCount.collectAsStateWithLifecycle()
    val isSomeoneTalking by viewModel.isSomeoneTalking.collectAsStateWithLifecycle()
    val cctvPhase by viewModel.cctvPhase.collectAsStateWithLifecycle()
    val arrestStatus by viewModel.arrestStatus.collectAsStateWithLifecycle()
    val arrestFailReason by viewModel.arrestFailReason.collectAsStateWithLifecycle()

    // ✅ 경고음/진동 매니저 (1번만 선언)

    // ✅ 테스트 플래그 (1번만 선언)
    val TEST_FORCE_BEEP = false

    // 테스트: 화면 진입 후 2초 뒤 1회 비프
    LaunchedEffect(TEST_FORCE_BEEP, role) {
        if (!TEST_FORCE_BEEP) return@LaunchedEffect
        if (role != GameRole.THIEF) return@LaunchedEffect
        delay(2000)
        feedbackManager.playBeepAlert(distance = 12.0)
    }

    LaunchedEffect(Unit) {
        viewModel.initGame()

        // 무전기 연결
        if (role == GameRole.POLICE) {
            delay(500) // 게임 정보 동기화 대기
            viewModel.connectWalkie()
        }

        // 게임 종료 후 다음 화면으로 이동
        viewModel.uiEvent.collect { event ->
            if (event is GameSessionEvent.NavigateToLoading) {
                showGameOverOverlay = true

                delay(5000L)

                onNavigateToLoading(event.gameId)
            }
        }
    }

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
            viewModel.disconnectWalkie()
        }
    }

    // beep 이벤트 collect (도둑만)
    LaunchedEffect(role) {
        if (role != GameRole.THIEF) return@LaunchedEffect

        viewModel.beepEvent.collect { beep ->
            Timber.d("🚨 beepEvent: policeId=${beep.policeId}, thiefId=${beep.thiefId}, distance=${beep.distance}")

            // distance 기반 난이도 패턴 적용
            feedbackManager.playBeepAlert(distance = beep.distance)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            if (escapeQueue.isNotEmpty() && !showThiefEscaped) {
                escapedThiefNickname = escapeQueue.first()
                showThiefEscaped = true

                delay(3000)

                showThiefEscaped = false

                viewModel.dequeEscape()

                delay(500)
            } else {
                delay(100)
            }
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

            // =========================================================
            // ===== TEST ONLY (BEEP UI) ================================
            // =========================================================
            if (TEST_FORCE_BEEP && role == GameRole.THIEF) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .systemBarsPadding()
                        .padding(top = 10.dp, end = 12.dp)
                        .clickable {
                            scope.launch {
                                Timber.d("🧪 TEST BEEP 버튼 클릭")
                                feedbackManager.playBeepAlert(distance = 8.0)
                            }
                        }
                ) {
                    PixelContainer(
                        modifier = Modifier,
                        backgroundColor = Color.Transparent,
                        borderColor = MissionYellow,
                        borderWidth = 6f
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            text = "TEST BEEP",
                            style = MaterialTheme.typography.titleSmall,
                            color = MissionYellow
                        )
                    }
                }
            }

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
                // 왼쪽: 지도
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

                // 오른쪽: 경찰 기능 버튼들
                if (role == GameRole.POLICE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                        // 카메라
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

                        // 도둑 리스트
                        PixelIconButton(
                            modifier = Modifier.size(50.dp),
                            borderColor = ButtonDisabled,
                            pixelSize = 3.dp,
                            onClick = {
                                phoneScreen = PhoneScreen.THIEF_LIST
                                clicked = !clicked
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_thief_list),
                                contentDescription = null,
                                tint = Color.Unspecified
                            )
                        }
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
                ContDownUI(remainingSeconds = 180)
                Spacer(modifier = Modifier.height(30.dp))

                FlipImage(
                    role = role,
                    memberId = myMemberId,
                    canFlip = isChief,
                    helicopterEnabled = helicopterEnabled,
                    onHelicopterClick = { viewModel.useHelicopterSkill() }
                )

            }
        }

        if (role == GameRole.THIEF) {
            MissionBottomSheet {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    items(missions) { mission ->
                        ExpandableCard(
                            modifier = Modifier,
                            title = mission.Mission.title,
                            disabled = mission.status == "SUCCESS",
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = mission.Mission.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )

                                PixelContainer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = { if (mission.status == "IN_PROGRESS") goToCamera(mission.id) }),
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

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }

        } else {
            WalkieBottomSheet {
                WalkieTalkieContent(
                    channel = when (walkieState) {
                        is WalkieConnectionState.Idle -> "CH 00 · 대기 중"
                        is WalkieConnectionState.Connecting -> "CH 00 · 연결 중..."
                        is WalkieConnectionState.Connected -> "CH 00 · MAIN (${walkieParticipantCount}명)"
                        is WalkieConnectionState.Error -> "CH 00 · 오류 발생"
                    },
                    isTalking = walkieMicEnabled,
                    isSomeoneTalking = isSomeoneTalking, // ✅ 추가
                    onPttDown = {
                        if (walkieConnected && !isSomeoneTalking) {
                            viewModel.startTalking()
                        } else if (isSomeoneTalking) {
                            Timber.d("📻 다른 경찰이 말하는 중 - PTT 무시")
                        }
                    },
                    onPttUp = {
                        if (walkieConnected) viewModel.stopTalking()
                    },
                    onChannelDown = { /* 채널 변경 (추후) */ },
                    onChannelUp = { /* 채널 변경 (추후) */ }
                )

            }

        }

    }

    // 경찰 헬기 안내 오버레이
    if (helicopterState == HelicopterPhase.NOTIFY && role == GameRole.POLICE) {
        WarningOverlay(
            onWarning = false,
            warningTitle = "경찰 헬기 지원!",
            warningMessage = "곧 공중 지원이\n도착합니다!"
        )
    }
    if (helicopterState == HelicopterPhase.REVEAL && role == GameRole.POLICE) {
        WarningOverlay(
            onWarning = false,
            warningTitle = "경찰 헬기 도착!",
            warningMessage = "모든 도둑의 위치가\n잠시동안 노출 됩니다!"
        )
    }

    // 경찰 CCTV 안내 오버레이
    if (cctvPhase == CctvPhase.NOTIFY && role == GameRole.POLICE) {
        WarningOverlay(
            onWarning = false,
            warningTitle = "영상 분석실에서 긴급연락 도착!",
            warningMessage = "CCTV 영상 분석중\n이상 징후를 포착했습니다!"
        )
    }
    if (cctvPhase == CctvPhase.REVEAL && role == GameRole.POLICE) {
        WarningOverlay(
            onWarning = false,
            warningTitle = "현상 수배범 포착!",
            warningMessage = "CCTV에 수배범이 찍혔습니다!\n수배범의 위치가 노출됩니다!"
        )
    }
    if (arrestStatus != ArrestStatus.IDLE) {
        AlertOverlay(
            title = if (arrestStatus == ArrestStatus.SUCCESS) "체포 성공!" else "체포 실패",
            message = if (arrestStatus == ArrestStatus.SUCCESS) "" else when(arrestFailReason) {
                "NOT_THIEF" -> "도둑이 아닙니다"
                "ARRESTER_NOT_POLICE" -> "경찰만 체포할 수 있습니다"
                "ALREADY_CAUGHT" -> "이미 체포된 도둑입니다"
                else -> ""
            },
            color = if (arrestStatus == ArrestStatus.SUCCESS) WinColor else LoseColor,
        )
    }


    // PhoneFrame (NPE 방지)
    if (clicked && currentLocation != null && prisonLocation != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            PhoneFrame(
                screen = phoneScreen,
                onScanSuccess = { thiefId ->
                    viewModel.arrestThief(thiefId.toLong())
                    phoneScreen = PhoneScreen.NO_SIGNAL
                },
                role = role,
                currentLocation = LatLng(currentLocation.latitude, currentLocation.longitude),
                areaPoints = areaPoints,
                prisonLocation = LatLng(prisonLocation.latitude, prisonLocation.longitude),
                thiefMembers = thiefMembers,
                playerLocations = if (role == GameRole.POLICE) memberLocation else emptyList()
            )
        }
    }

    // 경기구역이탈 오버레이
    if (warningReason.value == WarningReason.OUT_OF_BOUNDARY) {
        WarningOverlay(
            onWarning = true,
            warningTitle = "경기구역이탈!",
            warningMessage = "경기 구역으로\n복귀하세요"
        )
    }

    // 도둑 CCTV 발각 경고 오버레이
    if (cctvPhase == CctvPhase.REVEAL && role == GameRole.THIEF) {
        WarningOverlay(
            onWarning = true,
            warningTitle = "위치 노출!",
            warningMessage = "CCTV에 당신이 찍혔습니다!\n잠시동안 위치가 노출됩니다!"
        )
    }

    // 도둑 탈출 알림
    if (showThiefEscaped && role == GameRole.POLICE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.fillMaxHeight(0.75f))

            Text(
                text = "도둑이 탈출에\n성공했습니다!",
                fontFamily = PixelFont,
                color = Color.Yellow,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 45.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = escapedThiefNickname,
                fontFamily = PixelFont,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }

    // 미션 제출 결과 알림창
    Box (modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (missionState) {
            MissionStatus.IDLE -> {}
            MissionStatus.IN_ANALYZE -> {
                PixelLoading(message = "미션 수행중...")
            }

            MissionStatus.SUCCESS -> {
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .width(400.dp)
                ) {
                    PixelAlertDialog(
                        title = "미션 성공!",
                        message = "감시망을 교묘하게 피하는데 성공했습니다! \n 이제 더이상 CCTV에 노출되지 않습니다.",
                    ) {
                        PixelButtonCode(
                            text = "확인",
                            onClick = { viewModel.missionInit() }
                        )
                    }
                }
            }

            MissionStatus.FAIL -> {
                Box(
                    modifier = Modifier
                        .height(200.dp)
                        .width(400.dp)
                ) {
                    PixelAlertDialog(
                        title = "미션 실패",
                        message = missionFailReason,
                    ) {
                        PixelButtonCode(
                            text = "확인",
                            fontSize = 20,
                            onClick = { viewModel.missionInit() }
                        )
                    }
                }
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
