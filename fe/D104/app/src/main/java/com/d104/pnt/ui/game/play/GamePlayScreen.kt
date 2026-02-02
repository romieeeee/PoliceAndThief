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
    goToCamera: () -> Unit,
    viewModel: GamePlayViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var backPressedTime by remember { mutableLongStateOf(0L) }

    var clicked by remember { mutableStateOf(false) }
    var phoneScreen by remember { mutableStateOf(PhoneScreen.NO_SIGNAL) }

    val currentLocation = viewModel.userLocation.collectAsStateWithLifecycle().value
    val areaPoints = viewModel.polygonPoints.collectAsStateWithLifecycle().value
    val prisonLocation = viewModel.prisonLocation.collectAsStateWithLifecycle().value

    val thiefMembers by viewModel.thiefMembers.collectAsStateWithLifecycle()
    val isOutOfBoundary by viewModel.isOutOfBoundary.collectAsStateWithLifecycle()
    val minimapPlayers by viewModel.minimapPlayers.collectAsStateWithLifecycle()

    // 🚁 헬기 상태 + 권한 (ViewModel 제공값 사용)
    val helicopterState by viewModel.helicopterState.collectAsStateWithLifecycle()
    val isChief by viewModel.isChief.collectAsStateWithLifecycle()
    val helicopterEnabled by viewModel.helicopterButtonEnabled.collectAsStateWithLifecycle()

    val myMemberId by viewModel.myMemberId.collectAsStateWithLifecycle()

    val feedbackManager = remember { GameFeedbackManager(context.applicationContext) }

    val TEST_FORCE_BEEP = false

    LaunchedEffect(TEST_FORCE_BEEP, role) {
        if (!TEST_FORCE_BEEP) return@LaunchedEffect
        if (role != GameRole.THIEF) return@LaunchedEffect
        delay(2000)
        feedbackManager.playBeepAlert(distance = 12.0)
    }

    // 위치서비스 시작/종료
    var showGameOverOverlay by remember { mutableStateOf(false) }

    var showThiefEscaped by remember { mutableStateOf(false) }
    var escapedThiefNickname by remember { mutableStateOf("") }

    val escapeQueue by viewModel.escapeQueue.collectAsStateWithLifecycle()

    // ✅ ToneGenerator 직접 생성 금지 -> GameFeedbackManager로 통일
    // (테스트용이라도 여기서 직접 ToneGenerator 만들면 연타 시 AudioTrack(-12) 가능)
    val feedbackManager = remember { GameFeedbackManager(context.applicationContext) }

    // =========================================================
    // ===== TEST ONLY (BEEP) START =============================
    // 서버/소켓 이벤트 없이도 소리/진동이 나는지 확인하는 테스트 코드입니다.
    //
    // ✅ 사용법:
    // - TEST_FORCE_BEEP = true  : 테스트 활성화 (도둑 화면에서만)
    // - TEST_FORCE_BEEP = false : 테스트 비활성화 (실서버 이벤트만)
    //
    // ✅ 테스트 끝나면:
    // - TEST_FORCE_BEEP를 false로 돌리거나
    // - 이 블록(START~END) 통째로 삭제하면 됩니다.
    // =========================================================
    val TEST_FORCE_BEEP = false

    LaunchedEffect(TEST_FORCE_BEEP, role) {
        if (!TEST_FORCE_BEEP) return@LaunchedEffect
        if (role != GameRole.THIEF) return@LaunchedEffect

        // 화면 진입 후 2초 뒤 경고음 1번 (거리 12m 가정)
        delay(2000)
        feedbackManager.playBeepAlert(distance = 12.0)
    }
    // ===== TEST ONLY (BEEP) END ===============================
    // =========================================================


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

        onDispose { context.stopService(serviceIntent) }
    }

    // 게임 초기화 + UI 이벤트
    LaunchedEffect(Unit) {
        viewModel.initGame()

        viewModel.uiEvent.collect { event ->
            if (event is GameSessionEvent.NavigateToLoading) {
                showGameOverOverlay = true

                delay(5000L)

                onNavigateToLoading(event.gameId)
            }
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

                viewModel.removeFirstEscape()

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

            // 상단 버튼 영역

            // =========================================================
            // ===== TEST ONLY (BEEP UI) START ==========================
            // 화면에서 버튼 눌러서 수동으로 경고음/진동 테스트하는 UI 입니다.
            // 필요 없으면 이 블록만 삭제하면 됩니다.
            // =========================================================
            if (TEST_FORCE_BEEP && role == GameRole.THIEF) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .systemBarsPadding()
                        .padding(top = 10.dp, end = 12.dp)
                        .clickable {
                            // 클릭할 때마다 "아주 가까움" 패턴 테스트 (8m)
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
            // ===== TEST ONLY (BEEP UI) END ============================
            // =========================================================

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
                            painter = painterResource(
                                if (role == GameRole.POLICE) R.drawable.ic_thief_list
                                else R.drawable.ic_mission_list
                            ),
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
                ContDownUI(remainingSeconds = 180)
                Spacer(modifier = Modifier.height(30.dp))

                // 헬기는 FlipImage "뒷면"이 버튼 역할
                // - 청장이 아니면 아예 뒤집기 금지(canFlip=false)
                // - 버튼 비활성화 상태면 회색처리는 FlipImage 내부에서
                FlipImage(
                    role = role,
                    memberId = myMemberId, // ✅ TODO였던 값 수정: 내 memberId로
                    canFlip = isChief, // ✅ 청장만 뒤집기 가능
                    helicopterEnabled = helicopterEnabled, // ✅ 1회 사용 후 false
                    onHelicopterClick = { viewModel.useHelicopterSkill() }
                )

                // (선택) 상태 안내 텍스트는 원하면 유지, 싫으면 삭제
                // if (isChief && helicopterState.phase != HelicopterPhase.IDLE) { ... }
            }
        }

        // 하단 시트
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
                onScanSuccess = { result ->
                    phoneScreen = PhoneScreen.THIEF_LIST
                },
                role = role,
                currentLocation = LatLng(currentLocation.latitude, currentLocation.longitude),
                areaPoints = areaPoints,
                prisonLocation = LatLng(prisonLocation.latitude, prisonLocation.longitude),
                thiefMembers = thiefMembers,
                playerLocations = if (role == GameRole.POLICE) minimapPlayers else emptyList()
            )
        }
    }

    // 경기구역이탈 오버레이
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
