package com.d104.pnt.ui.game.play

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.base.Constants
import com.d104.pnt.data.remote.model.response.BeepUseResponse
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRepository
import com.d104.pnt.data.repository.GameSessionEvent
import com.d104.pnt.data.repository.GameSessionRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.data.repository.WalkieRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.service.game.GameActiveService
import com.d104.pnt.util.StepSensorManager
import com.d104.pnt.util.socket.GameSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
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
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GamePlayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val authRepository: AuthRepository,
    private val gameSocketManager: GameSocketManager,
    savedStateHandle: SavedStateHandle,
    private val gameRepository: GameRepository,
    private val gameSessionRepository: GameSessionRepository,
    private val stepSensorManager: StepSensorManager,
    private val walkieRepository: WalkieRepository
) : ViewModel() {
    private val gameId: Long = savedStateHandle.get<Long>(NavArgs.GAME_ID) ?: 0L
    private val roleString: String = savedStateHandle.get<String>(NavArgs.ROLE) ?: "THIEF"

    private val _uiEvent = MutableSharedFlow<GameSessionEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val uiEvent = _uiEvent.asSharedFlow()
    val isOutOfBoundary = gameSessionRepository.isOutOfBoundary

    // ===== Beep 이벤트 (도둑 쪽에서만 화면이 소리 재생하도록 Screen에서 필터) =====
    private val _beepEvent = MutableSharedFlow<BeepUseResponse>(extraBufferCapacity = 16)
    val beepEvent = _beepEvent.asSharedFlow()

    val userLocation = locationRepository.currentLocation
    val polygonPoints = locationRepository.polygonPoints
    val prisonLocation = locationRepository.prisonLocation

    private val _allMembers = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())

    val members = gameSessionRepository.members

    val gameStatus = gameSessionRepository.gameStatus

    val missions = gameSessionRepository.missions

    private val _thiefMembers = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())
    val thiefMembers = members.map { list ->
        list.filter { it.position.equals("THIEF", ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isOutOfBoundary = MutableStateFlow(false)

    private val _escapeQueue = MutableStateFlow<List<String>>(emptyList())
    val escapeQueue = _escapeQueue.asStateFlow()

    // ===== 무전기 =====
    val walkieConnected = walkieRepository.isConnected
    val walkieMicEnabled = walkieRepository.isMicEnabled
    val walkieParticipantCount = walkieRepository.participantCount

    private val _walkieState = MutableStateFlow<WalkieConnectionState>(WalkieConnectionState.Idle)
    val walkieState = _walkieState.asStateFlow()

    private val _isSomeoneTalking = MutableStateFlow(false)

    val isSomeoneTalking = _isSomeoneTalking.asStateFlow()

    private val _talkingMemberId = MutableStateFlow<Long?>(null)

    val talkingMemberId = _talkingMemberId.asStateFlow()

    private var myMemberId: Long = 0L

    private var warningJob: Job? = null

    init {
        setupSocketListeners()
        gameSessionRepository.gameInit()
        observeRepositoryEvents()
        startService(GameActiveService.ACTION_START)
    }

    private fun observeRepositoryEvents() {
        viewModelScope.launch {
            gameSessionRepository.eventFlow.collect { event ->
                when (event) {
                    is GameSessionEvent.NavigateToLoading -> {
                        _uiEvent.emit(event)
                    }

                    is GameSessionEvent.NavigateToNews -> {
                        _uiEvent.emit(event)
                    }

                    is GameSessionEvent.GameStarted -> {
                        startService(GameActiveService.ACTION_START)
                    }

                    else -> {
                        Timber.d("기타 이벤트 처리: $event")
                    }
                }
            }
        }
    }

    private fun fetchMyId() {
        viewModelScope.launch {
            authRepository.getMemberId().collect { id ->
                if (id != 0L) myMemberId = id
            }
        }
    }


    fun initGame() {
        viewModelScope.launch {
            val token = authRepository.getAccessToken().first()
            if (token.isNotEmpty() && !gameSocketManager.isConnected()) {
                gameSocketManager.connect(token)
                while (!gameSocketManager.isConnected()) {
                    delay(100)
                }
            }
            gameSocketManager.joinGame(gameId)
            delay(300)
            gameSocketManager.syncGameInfo()
        }
    }

    private fun setupSocketListeners() {
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

        // 전체 게임 정보 동기화
        gameSocketManager.setOnGameInfoSynced { data ->
            viewModelScope.launch {
                try {
                    val membersArray = data.optJSONArray("members")
                    if (membersArray != null) {
                        val newMembers = mutableListOf<GameMemberSocketDto>()
                        for (i in 0 until membersArray.length()) {
                            val memberJson = membersArray.getJSONObject(i)
                            newMembers.add(GameMemberSocketDto.fromJson(memberJson))
                        }

                        updateMembersList(newMembers)
                        Timber.d("GamePlayViewModel: 전체 멤버 동기화 완료 (${newMembers.size}명)")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "GamePlayViewModel: 게임 정보 파싱 실패")
                }
            }
        }

        // 실시간 상태 변경
        gameSocketManager.setOnMemberStatusChanged { gameId, thiefId, status, arrestedAt ->
            viewModelScope.launch {
                val currentList = _allMembers.value.toMutableList()
                val targetIndex = currentList.indexOfFirst { it.memberId == thiefId }

                if (targetIndex != -1) {
                    val oldData = currentList[targetIndex]
                    val newData = oldData.copy(rawStatus = status)
                    currentList[targetIndex] = newData

                    updateMembersList(currentList)
                    Timber.d("GamePlayViewModel: 도둑($thiefId) 상태 변경 -> $status")
                }
            }
        }

        // 도둑 탈출 수신
        gameSocketManager.setOnThiefEscaped { gameId, thiefId, escapedAt ->
            viewModelScope.launch {
                Timber.d("🏃 도둑 탈출 알림 수신: thiefId=$thiefId, escapedAt=$escapedAt")

                val escapedThief = _allMembers.value.find { it.memberId == thiefId }
                val thiefNickname = escapedThief?.nickname ?: "도둑"

                _escapeQueue.value = _escapeQueue.value + thiefNickname
                Timber.d("📋 탈출 큐에 추가: $thiefNickname (현재 큐 크기: ${_escapeQueue.value.size})")
            }
        }

        gameSocketManager.setOnRadioReceived { gameId, memberId ->
            viewModelScope.launch {
                Timber.d("📻 다른 경찰(memberId=$memberId)이 말하기 시작")
                _isSomeoneTalking.value = true
                _talkingMemberId.value = memberId
            }
        }
    }

    fun removeFirstEscape() {
        _escapeQueue.value = _escapeQueue.value.drop(1)
        Timber.d("📋 탈출 큐에서 제거 (남은 큐 크기: ${_escapeQueue.value.size})")
    }

    private fun updateMembersList(newList: List<GameMemberSocketDto>) {
        _allMembers.value = newList

        newList.forEach { member ->
            Timber.d("🕵️ 멤버 확인: ${member.nickname} / 포지션: [${member.position}] / 상태: ${member.rawStatus}")
        }

        _thiefMembers.value = newList.filter {
            it.position.equals("THIEF", ignoreCase = true)
        }

        Timber.d("📋 필터링된 도둑 수: ${_thiefMembers.value.size}명")
    }

    private fun startService(action: String) {
        Intent(context, GameActiveService::class.java).also { intent ->
            intent.action = action

            if (action == GameActiveService.ACTION_STOP) {
                context.startService(intent)
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)

                }
            }
        }
    }

    fun manualLeaveGame() {
        viewModelScope.launch {
            Timber.d("🚪 유저가 직접 게임 종료를 선택함")
            // 1. GPS 서비스 중단
            startService(GameActiveService.ACTION_STOP)
            // 2. 소켓 연결 해제 및 세션 정리 (post disconnect 포함)
            gameSessionRepository.leaveGame()
        }
    }

    /**
     * ====================
     * 무전기 연결 (경찰만)
     * ====================
     */
    fun connectWalkie() {
        // 경찰이 아니면 무시
        if (roleString != "POLICE") {
            Timber.d("🎙️ 도둑은 무전기 연결 안함")
            return
        }

        viewModelScope.launch {
            try {
                _walkieState.value = WalkieConnectionState.Connecting

                // 1. roomCode 가져오기
                val roomCode = gameSessionRepository.roomCode.first()
                if (roomCode.isBlank()) {
                    _walkieState.value = WalkieConnectionState.Error("방 코드가 없습니다")
                    Timber.e("🎙️ 무전기 연결 실패: roomCode 없음")
                    return@launch
                }

                Timber.d("🎙️ LiveKit 토큰 요청: roomCode=$roomCode")

                // 2. 토큰 발급
                when (val result = gameRepository.getLiveKitToken(roomCode)) {
                    is BaseResult.Success -> {
                        val tokenResponse = result.data
                        Timber.d("🎙️ 토큰 발급 성공: ${tokenResponse.identity}")

                        // 3. LiveKit 연결
                        walkieRepository.connect(
                            serverUrl = Constants.LIVEKIT_URL,
                            token = tokenResponse.token,
                            roomName = tokenResponse.roomCode
                        )

                        _walkieState.value = WalkieConnectionState.Connected
                        Timber.d("🎙️ 무전기 연결 완료")
                    }

                    is BaseResult.Error -> {
                        val errorMsg = result.error.message ?: "토큰 발급 실패"
                        _walkieState.value = WalkieConnectionState.Error(errorMsg)
                        Timber.e("🎙️ 토큰 발급 실패: $errorMsg")
                    }
                }
            } catch (e: Exception) {
                _walkieState.value = WalkieConnectionState.Error(e.message ?: "연결 실패")
                Timber.e(e, "🎙️ 무전기 연결 실패")
            }
        }
    }

    fun disconnectWalkie() {
        viewModelScope.launch {
            try {
                walkieRepository.disconnect()
                _walkieState.value = WalkieConnectionState.Idle
                Timber.d("🎙️ 무전기 연결 해제")
            } catch (e: Exception) {
                Timber.e(e, "🎙️ 연결 해제 실패")
            }
        }
    }

    fun startTalking() {
        if (roleString != "POLICE") return

        viewModelScope.launch {
            try {
                // 1. 소켓으로 "내가 말한다" 신호 보내기
                gameSocketManager.sendRadio()

                // 2. LiveKit 마이크 켜기
                walkieRepository.enableMic()

                Timber.d("🎙️ 송신 시작 (소켓 + LiveKit)")
            } catch (e: Exception) {
                Timber.e(e, "🎙️ PTT 시작 실패")
            }
        }
    }

    fun stopTalking() {
        if (roleString != "POLICE") return

        viewModelScope.launch {
            try {
                // LiveKit 마이크만 끄기 (소켓은 post radio 없음)
                walkieRepository.disableMic()

                // 내 상태 초기화
                _isSomeoneTalking.value = false
                _talkingMemberId.value = null

                Timber.d("🎙️ 송신 중지")
            } catch (e: Exception) {
                Timber.e(e, "🎙️ PTT 중지 실패")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        disconnectWalkie()
        startService(GameActiveService.ACTION_STOP)
    }
}

sealed class WalkieConnectionState {
    object Idle : WalkieConnectionState()
    object Connecting : WalkieConnectionState()
    object Connected : WalkieConnectionState()
    data class Error(val message: String) : WalkieConnectionState()
}