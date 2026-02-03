package com.d104.pnt.data.repository

import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.BeepUseResponse
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.remote.model.response.MemberLocationSocketDto
import com.d104.pnt.data.remote.model.response.MissionSocketDto
import com.d104.pnt.util.StepSensorManager
import com.d104.pnt.util.socket.GameSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
) : GameSessionRepository {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 실시간 데이터를 저장할 메모리 공간
    private val _gameId = MutableStateFlow(0L)
    override val gameId = _gameId.asStateFlow()
    private val _gameStatus = MutableStateFlow("")
    override val gameStatus = _gameStatus.asStateFlow()

    private val _gameTime = MutableStateFlow(0)
    override val gameTime = _gameTime.asStateFlow()

    private val _missions = MutableStateFlow<List<MissionSocketDto>>(emptyList())
    override val missions = _missions.asStateFlow()
    private val _members = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())
    override val members = _members.asStateFlow()

    private val _memberLocation = MutableStateFlow<List<MemberLocationSocketDto>>(emptyList())
    override val memberLocation = _memberLocation.asStateFlow()

    override val thiefMembers = _members.map { list ->
        list.filter { it.position.equals("THIEF", ignoreCase = true) }
    }.stateIn(
        scope = repositoryScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _eventFlow = MutableSharedFlow<GameSessionEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val eventFlow = _eventFlow.asSharedFlow()

    private val _isOutOfBoundary = MutableStateFlow(false)
    override val isOutOfBoundary = _isOutOfBoundary.asStateFlow()

    private var isConnecting = false

    private val _myMemberId = MutableStateFlow(0L)
    override val myMemberId = _myMemberId.asStateFlow()

    private val _myRole = MutableStateFlow("")
    override val myRole = _myRole.asStateFlow()

    private val _escapeQueue = MutableStateFlow<List<String>>(emptyList())
    override val escapeQueue = _escapeQueue.asStateFlow()

    private val _beepEvent = MutableSharedFlow<BeepUseResponse>(extraBufferCapacity = 16)
    override val beepEvent = _beepEvent.asSharedFlow()

    private val _baseTime = MutableStateFlow(0) // 탈옥 시 해당 시간으로 초기화
    private val _survivalTime = MutableStateFlow(0)
    override val survivalTime = _survivalTime.asStateFlow()

    private val _longestSurvivalTime = MutableStateFlow(0)
    override val longestSurvivalTime = _longestSurvivalTime.asStateFlow()


    override fun setMemberId(memberId: Long) {
        _myMemberId.value = memberId
    }

    override fun setFinalRole(role: String) {
        _myRole.value = role
    }

    private var gpsJob: Job? = null
    private var warningJob: Job? = null
    private var gameStartTime: Long = 0L


    override fun gameInit() {
        repositoryScope.launch {
            setupSocketListeners()

            delay(500L)
            gameSocketManager.syncGameInfo()
        }
        stepSensorManager.startListening()

    }

    override fun connectAndJoin(gameId: Long) {
        if (isConnecting || gameSocketManager.isConnected()) return

        repositoryScope.launch {
            isConnecting = true

            try {
                // 1. 리스너부터 확실히 먼저 등록 (신호를 놓치지 않게)
                gameSocketManager.removeAllListeners()
                setupSocketListeners()

                // 2. 연결 시도
                val token = authRepository.getAccessToken().first()
                gameSocketManager.connect(token)

                // 3. 연결될 때까지 대기
                var retry = 0
                while (!gameSocketManager.isConnected() && retry < 30) {
                    delay(100)
                    retry++
                }

                if (gameSocketManager.isConnected()) {
                    // 4. 연결 성공 후 딱 한 번만 Join 요청
                    gameSocketManager.joinGame(gameId)
                }
            } finally {
                isConnecting = false
            }
        }
    }

    private fun setupSocketListeners() {
        gameSocketManager.setOnGameStarted { gameId, startTime ->
            _gameId.value = gameId
            repositoryScope.launch {
                // 뷰모델에게 "넘어가라"고 신호 보냄
                _eventFlow.emit(GameSessionEvent.GameStarted(gameId, startTime))
            }
        }
        gameSocketManager.setOnGpsReceived {cctvThiefId, skillUsedAt, sec, locations ->
            _gameTime.value = sec
            try {
                if (locations.length() != 0) {
                    val newMemberLocation = mutableListOf<MemberLocationSocketDto>()
                    for (i in 0 until locations.length()) {
                        val locationJson = locations.getJSONObject(i)
                        if (cctvThiefId != null) {
                            if (locationJson.optLong("memberId") == cctvThiefId) {
                                locationJson.put("status", "CCTV")
                            }
                        }
                        newMemberLocation.add(MemberLocationSocketDto.fromJson(locationJson))
                    }
                    _memberLocation.value = newMemberLocation
                    Timber.d("멤버 위치 파싱 완료: ${newMemberLocation}")
                }
            }
            catch (e:Exception) {
                Timber.e(e, "멤버 위치 파싱 실패")
            }
        }

        gameSocketManager.setOnJoinedRoom { gameId, memberId, message ->
            _gameId.value = gameId
        }
        gameSocketManager.setOnWillStartGame { gameId, willStartAt ->
            Timber.d("socket ⏰ get will start game 수신 - gameId: $gameId, willStartAt: $willStartAt")
        }

        // 전체 게임 정보 동기화
        gameSocketManager.setOnGameInfoSynced { data ->
            try {
                Timber.d("전체 데이터: $data")
                val gameId = data.optLong("gameId")
                if (gameId != 0L) _gameId.value = gameId // ⭐ 저장

                val gameStatus = data.optString("gameStatus")
                if (gameStatus != "") _gameStatus.value = gameStatus

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
                val missionArray = data.optJSONArray("missions")
                if (missionArray != null) {
                    val newMissions = mutableListOf<MissionSocketDto>()
                    for (i in 0 until missionArray.length()) {
                        val missionJson = missionArray.getJSONObject(i)
                        newMissions.add(MissionSocketDto.fromJson(missionJson))
                    }
                    _missions.value = newMissions
                    Timber.d("GamePlayViewModel: 전체 미션 동기화 완료 (${newMissions})")
                }
            } catch (e: Exception) {
                Timber.e(e, "GamePlayViewModel: 게임 정보 파싱 실패")
            }
        }

        // 실시간 상태 변경
        gameSocketManager.setOnMemberStatusChanged { gameId, thiefId, status, arrestedAt ->
            repositoryScope.launch {
                _members.update { currentList ->
                    currentList.map { member ->
                        if (member.memberId == thiefId) {
                            member.copy(rawStatus = status)
                        }
                        else member
                    }
                }
                Timber.d("GamePlayViewModel: 도둑($thiefId) 상태 변경 -> $status")
            }
        }

        // 도둑 탈출 수신
        gameSocketManager.setOnThiefEscaped { gameId, thiefId, escapedAt ->
            repositoryScope.launch {
                Timber.d("🏃 도둑 탈출 알림 수신: thiefId=$thiefId, escapedAt=$escapedAt")

                val escapedThief = members.value.find { it.memberId == thiefId }
                val thiefNickname = escapedThief?.nickname ?: "도둑"

                _escapeQueue.update {it + thiefNickname}
                Timber.d("📋 탈출 큐에 추가: $thiefNickname (현재 큐 크기: ${_escapeQueue.value.size})")
            }
        }

        // 비프음 수신 (GameSocketManager에서 이미 get beep use 파싱/콜백 호출 중)
        gameSocketManager.setOnBeepReceived { policeId, thiefId, distance ->
            _beepEvent.tryEmit(
                BeepUseResponse(
                    policeId = policeId,
                    thiefId = thiefId,
                    distance = distance
                )
            )
            Timber.d("📢 beep 수신: policeId=$policeId, thiefId=$thiefId, distance=$distance")
        }

        // 게임 종료 수신 -> 상세 결과 요청
        gameSocketManager.setOnGameEnded { winnerPosition, _ ->
            Timber.d("socket 🏁 게임 종료: $winnerPosition 승리 -> 상세 결과 요청")
            stopGameSession()
            gameSocketManager.postAfterGameEnd(_gameId.value)
        }

        // 상세 결과 수신 -> 이동 이벤트 발송
        gameSocketManager.setOnEndGameAfter { data ->
            repositoryScope.launch {
                _eventFlow.emit(GameSessionEvent.NavigateToLoading(_gameId.value))
            }
        }

        // 뉴스 생성 완료 수신
        gameSocketManager.setOnNewsReceived { gameId, newsId ->
            repositoryScope.launch {
                Timber.d("socket 📰 뉴스 도착 알림 수신: newsId=$newsId")
                _eventFlow.emit(GameSessionEvent.NavigateToNews(gameId, newsId))
            }
        }

        // 경게 벗어남 이벤트 수신 ->  경고 오버레이 띄움
        gameSocketManager.setOnOutOfBoundary { gameId, memberId ->
            Timber.w("⚠️ 경고: 구역 이탈 발생! (Game: $gameId)")
            showWarningEffect()
        }
    }

    override fun startGameSession() {
        if (gpsJob?.isActive == true) return

        gpsJob?.cancel()

        // 걸음 수 리셋
        stepSensorManager.resetGameSteps()
        gameStartTime = System.currentTimeMillis()

        gpsJob = repositoryScope.launch {
            // gameId가 0보다 커질 때까지 대기
            gameId.first { it > 0 }
            Timber.d("socket 🚀 레포지토리: GPS 전송 시작")
            while (isActive) {
                val location = locationRepository.currentLocation.value
                // 리셋된 걸음 수를 가져옴 (StepSensorManager에 gameStepCountFlow가 있다고 가정)
                val steps = stepSensorManager.stepCountFlow.value

                if (location != null) {

                    gameSocketManager.sendGPS(
                        lat = location.latitude,
                        lng = location.longitude,
                        walk = steps,
                        longestSurvived = _longestSurvivalTime.value
                    )
                    Timber.d("socket sendGPS: $location, $steps, $_longestSurvivalTime")
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
        stepSensorManager.stopListening()
        Timber.d("socket 레포지토리: GPS 전송 중단")
    }

    override fun leaveGame() {
        stopGameSession()

        gameSocketManager.leaveGame()

        _members.value = emptyList()
        _gameId.value = 0L
    }

    private fun showWarningEffect() {
        warningJob?.cancel()
        warningJob = repositoryScope.launch {
            _isOutOfBoundary.value = true
            delay(3000) // 3초간 유지
            _isOutOfBoundary.value = false
        }
    }

    override fun dequeEscape(){
        _escapeQueue.value = _escapeQueue.value.drop(1)
        Timber.d("📋 탈출 큐에서 제거 (남은 큐 크기: ${_escapeQueue.value.size})")
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

    data class NavigateToLoading(val gameId: Long) : GameSessionEvent()
    data class NavigateToNews(val gameId: Long, val newsId: Long) : GameSessionEvent() // 실제 뉴스로 이동
}