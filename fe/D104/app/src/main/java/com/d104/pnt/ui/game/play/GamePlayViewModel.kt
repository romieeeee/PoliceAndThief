package com.d104.pnt.ui.game.play

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.BeepUseResponse
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRepository
import com.d104.pnt.data.repository.GameSessionEvent
import com.d104.pnt.data.repository.GameSessionRepository
import com.d104.pnt.data.repository.LocationRepository
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
import org.json.JSONArray
import org.json.JSONObject

@HiltViewModel
class GamePlayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val authRepository: AuthRepository,
    private val gameSocketManager: GameSocketManager,
    savedStateHandle: SavedStateHandle,
    private val gameRepository: GameRepository,
    private val gameSessionRepository: GameSessionRepository,
    private val stepSensorManager: StepSensorManager
) : ViewModel() {
    private val gameId: Long = savedStateHandle.get<Long>(NavArgs.GAME_ID) ?: 0L

    // UI 이벤트
    private val _uiEvent = MutableSharedFlow<GameSessionEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val uiEvent = _uiEvent.asSharedFlow()
    val isOutOfBoundary = gameSessionRepository.isOutOfBoundary

    // Beep 이벤트 (도둑 쪽에서만 화면이 소리 재생하도록 Screen에서 필터)
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

    private var myMemberId: Long = 0L
    private var warningJob: Job? = null


    private var myArrestCount = 0

    init {
        fetchMyId()
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
        // 비프음 수신
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

        // ✅ [추가] 내가 도둑을 잡았을 때 카운트 증가 (경찰용)
        // (주의: GameSocketManager에 setOnArrestResult 리스너가 있어야 합니다. 없으면 추가 필요)
        gameSocketManager.setOnArrestResult { result, _, policeId, _, _ ->
            if (result == "SUCCESS" && policeId == myMemberId) {
                myArrestCount++
                Timber.d("👮 내 체포 카운트 증가: $myArrestCount")
            }
        }

        gameSocketManager.setOnGameEnded { winTeam, data ->
            viewModelScope.launch {
                Timber.d("🏁 게임 종료 처리 시작")

                // 1. 내 기록 저장
                saveMyStatToRepository(data)

                // 2. 화면 이동 (showGameOverOverlay는 Composable State이므로 여기서 변경 불가)
                // 대신 NavigateToLoading 이벤트를 Screen에서 받아서 Overlay를 띄우도록 합니다.
                // Screen에서 이미 그렇게 구현되어 있습니다.
                _uiEvent.emit(GameSessionEvent.NavigateToLoading(gameId))
            }
        }
    }

    // 내 기록 파싱
    private fun saveMyStatToRepository(data: JSONObject) {
        try {
            val memberStats = data.optJSONArray("memberStats")
            var myStatString = "기록 없음"

            if (memberStats != null) {
                for (i in 0 until memberStats.length()) {
                    val stat = memberStats.getJSONObject(i)

                    // 내 ID와 일치하는지 확인
                    val id = stat.optLong("memberId", -1L)
                    // 만약 0이 나온다면 gameMemberId일 수도 있으니 확인 필요
                    val gameMemberId = stat.optLong("gameMemberId", -1L)

                    // 내 아이디와 매칭 (안전하게 둘 중 하나라도 맞으면)
                    if (id == myMemberId || (gameMemberId != -1L && gameMemberId == myMemberId)) {

                        val position = stat.optString("position")

                        if (position == "THIEF") {
                            val survived = stat.optInt("longestSurvived", 0)
                            val min = survived / 60
                            val sec = survived % 60
                            myStatString = String.format(java.util.Locale.getDefault(), "%02d:%02d", min, sec)
                        } else {
                            // [경찰]
                            val serverCount = stat.optInt("arrestCount", -1)

                            if (serverCount != -1) {
                                myStatString = "${serverCount}명"
                            } else {
                                myStatString = "${myArrestCount}명"
                            }
                        }
                        break
                    }
                }
            }

            // 저장!
            gameRepository.myLastGameStat = myStatString
            Timber.d("💾 내 기록 저장 완료: $myStatString (ID: $myMemberId)")

        } catch (e: Exception) {
            Timber.e(e, "기록 저장 실패")
            val isPolice = thiefMembers.value.none { it.memberId == myMemberId }
            gameRepository.myLastGameStat = if (isPolice) "0명" else "00:00"
        }
    }

    fun removeFirstEscape() {
        _escapeQueue.value = _escapeQueue.value.drop(1)
        Timber.d("📋 탈출 큐에서 제거 (남은 큐 크기: ${_escapeQueue.value.size})")
    }

    private fun updateMembersList(newList: List<GameMemberSocketDto>) {
        _allMembers.value = newList

        _thiefMembers.value = newList.filter {
            it.position.equals("THIEF", ignoreCase = true)
        }

        Timber.d("📋 필터링된 도둑 수: ${_thiefMembers.value.size}명")
    }

    override fun onCleared() {
        super.onCleared()
        startService(GameActiveService.ACTION_STOP)
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
}