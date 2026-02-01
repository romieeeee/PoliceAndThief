package com.d104.pnt.data.repository

import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.remote.model.response.MissionSocketDto
import com.d104.pnt.util.StepSensorManager
import com.d104.pnt.util.socket.GameSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameSessionRepositoryImpl @Inject constructor(
    private val gameSocketManager: GameSocketManager,
    private val stepSensorManager: StepSensorManager,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository
): GameSessionRepository {
    // 실시간 데이터를 저장할 메모리 공간
    private val _gameId = MutableStateFlow(0L)
    override val gameId = _gameId.asStateFlow()
    private val _gameStatus = MutableStateFlow("")
    override val gameStatus = _gameStatus.asStateFlow()
    private val _missions = MutableStateFlow<List<MissionSocketDto>>(emptyList())
    override val missions = _missions.asStateFlow()
    private val _members = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())
    override val members = _members.asStateFlow()

    private val _eventFlow = MutableSharedFlow<GameSessionEvent>()
    override val eventFlow = _eventFlow.asSharedFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var gpsJob: Job? = null

    private var gameStartTime: Long = 0L

    override fun gameInit(){
        // 1. 기존 리스너 정리 (혹시 남아있을 수 있으므로)
        gameSocketManager.removeAllListeners()
        // 2. 리스너 등록
        setupSocketListeners()
        // 3. 정보 동기화 요청 (약간의 딜레이 후)
        repositoryScope.launch {
            delay(500L)
            gameSocketManager.syncGameInfo()
        }
    }

    override fun connectAndJoin(gameId: Long) {
        repositoryScope.launch {
            // 1. 소켓 연결 시도
            try {
                val token = authRepository.getAccessToken().first() // 토큰 가져오기
                if (token.isNotEmpty()) {
                    Timber.d("🔌 레포지토리: 소켓 연결 시도...")
                    gameSocketManager.connect(token)

                    // 연결될 때까지 잠시 대기 (타임아웃 5초 설정)
                    var retry = 0
                    while (!gameSocketManager.isConnected() && retry < 50) {
                        delay(100)
                        retry++
                    }
                }
                else {
                    Timber.d("레포지토리: 토큰이 비어있습니다")
                }
            }
            catch (exception: Exception) {
                Timber.e("소켓 연결 실패. 입장을 중단합니다. message: ${exception.message}")
                return@launch
            }

            // 2. 연결 실패 시 중단
            if (!gameSocketManager.isConnected()) {
                Timber.e("❌ 소켓 연결 실패. 입장을 중단합니다.")
                return@launch
            }

            // 3. 리스너 세팅 (기존 것 지우고 새로 등록)
            gameSocketManager.removeAllListeners()
            setupSocketListeners() // 여기에 setOnGameStarted 등 포함됨

            // 4. 게임 입장 요청
            Timber.d("🚪 게임($gameId) 입장 요청")
            gameSocketManager.joinGame(gameId)

            // 5. [안전장치] 방장이 5초 이벤트를 놓쳤을 경우를 대비한 동기화 요청
            delay(500)
            gameSocketManager.syncGameInfo()
        }
    }

    private fun setupSocketListeners() {
        gameSocketManager.setOnGameStarted { gameId, startTime ->
            repositoryScope.launch {
                // 뷰모델에게 "넘어가라"고 신호 보냄
                _eventFlow.emit(GameSessionEvent.GameStarted(gameId, startTime))
            }
        }

        gameSocketManager.setOnJoinedRoom { gameId, memberId, message -> }

        gameSocketManager.setOnWillStartGame { gameId, willStartAt ->
            Timber.d("⏰ get will start game 수신 - gameId: $gameId, willStartAt: $willStartAt")
        }

        gameSocketManager.setOnGameInfoSynced { data ->
            try {
                val membersArray = data.optJSONArray("members")
                if (membersArray != null) {
                    val newMembers = mutableListOf<GameMemberSocketDto>()
                    for (i in 0 until membersArray.length()) {
                        val memberJson = membersArray.getJSONObject(i)
                        newMembers.add(GameMemberSocketDto.fromJson(memberJson))
                    }

                    _members.value = newMembers
                    Timber.d("GamePlayViewModel: 전체 멤버 동기화 완료 (${newMembers.size}명)")
                }
            } catch (e: Exception) {
                Timber.e(e, "GamePlayViewModel: 게임 정보 파싱 실패")
            }
        }

        // 실시간 상태 변경
        gameSocketManager.setOnMemberStatusChanged { gameId, thiefId, status, arrestedAt ->
            _members.update { currentList ->
                // currentList는 현재 시점의 최신 데이터임이 보장됨
                currentList.map { member ->
                    if (member.memberId == thiefId) {
                        member.copy(rawStatus = status)
                    } else {
                        member
                    }
                }
            }
        }

    }

    override fun startGameSession(){
        if (gpsJob?.isActive == true) return

        gpsJob?.cancel()

        // 걸음 수 리셋 (이전에 만든 기능 활용)
        stepSensorManager.resetGameSteps()
        gameStartTime = System.currentTimeMillis()

        gpsJob = repositoryScope.launch {
            Timber.d("🚀 레포지토리: GPS 전송 시작")
            while (isActive) {
                val location = locationRepository.currentLocation.value
                // 리셋된 걸음 수를 가져옴 (StepSensorManager에 gameStepCountFlow가 있다고 가정)
                val steps = stepSensorManager.stepCountFlow.value

                if (location != null) {
                    val longestSurvived =
                        ((System.currentTimeMillis() - gameStartTime) / 1000).toInt()

                    gameSocketManager.sendGPS(
                        lat = location.latitude,
                        lng = location.longitude,
                        walk = steps,
                        longestSurvived = longestSurvived
                    )
                }
                delay(1000L)
            }
        }
    }

    // 서비스 종료 시 호출할 함수
    override fun stopGameSession() {
        gpsJob?.cancel()
        gpsJob = null
        gameStartTime = 0L
        Timber.d("레포지토리: GPS 전송 중단")
    }

    override fun leaveGame() {
        stopGameSession() // 안전하게 트래킹 종료
        gameSocketManager.leaveGame()
        _members.value = emptyList()
    }
}

sealed class GameSessionEvent {

    // 1. 게임 시작 신호 (게임 ID와 시작 시간을 담아서 보냄)
    data class GameStarted(
        val gameId: Long,
        val startTime: String
    ) : GameSessionEvent()

    // 2. (예시) 게임 종료 신호
    data object GameEnded : GameSessionEvent()

    // 3. (예시) 에러 발생 신호
    data class ErrorOccurred(val message: String) : GameSessionEvent()
}