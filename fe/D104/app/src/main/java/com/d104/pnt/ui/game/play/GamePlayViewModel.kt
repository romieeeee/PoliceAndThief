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
import com.d104.pnt.util.getSingleLocation
import com.d104.pnt.util.socket.GameSocketManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
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
    private val stepSensorManager: StepSensorManager
) : ViewModel() {

    private val gameId: Long = savedStateHandle.get<Long>(NavArgs.GAME_ID) ?: 0L

    // UI 이벤트
    private val _uiEvent = MutableSharedFlow<GamePlayUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()
    val isOutOfBoundary = gameSessionRepository.isOutOfBoundary

    // ✅ Beep 이벤트 (도둑 쪽에서만 화면이 소리 재생하도록 Screen에서 필터)
    private val _beepEvent = MutableSharedFlow<BeepUseResponse>(extraBufferCapacity = 16)
    val beepEvent = _beepEvent.asSharedFlow()

    val userLocation = locationRepository.currentLocation
    val polygonPoints = locationRepository.polygonPoints
    val prisonLocation = locationRepository.prisonLocation

    private val _allMembers = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())

    val members = gameSessionRepository.members

//    val gameRepoId = gameSessionRepository.gameId

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

    init {
        setupSocketListeners()
        gameSessionRepository.gameInit()
        observeRepositoryEvents()
        startService(GameActiveService.ACTION_START)
        Timber.d("GameAction 시작")
    }

    private fun observeRepositoryEvents() {
        viewModelScope.launch {
            gameSessionRepository.eventFlow.collect { event ->
                when (event) {
                    is GameSessionEvent.NavigateToNews -> {
                        Timber.d("📰 뉴스 화면 이동 이벤트 수신")
                        _uiEvent.emit(GamePlayUiEvent.NavigateToNews(event.gameId))
                    }

                    is GameSessionEvent.GameStarted -> {
                        // 혹시 재접속해서 들어온 경우 여기서 서비스 시작 가능
                        startService(GameActiveService.ACTION_START)
                    }

                    is GameSessionEvent.GameEnded -> {
                        Timber.d("SessionEvent: GameEnded")
                    }

                    is GameSessionEvent.ErrorOccurred -> {
                        Timber.d("SessionEvent: ErrorOccurred - ${event.message}")
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

        // 게임 종료 수신 → 상세결과 요청
        gameSocketManager.setOnGameEnded { winnerPosition, message ->
            Timber.d("🏁 게임 종료 수신: $winnerPosition 승리")

            // 상세 결과 요청 (post after game end)
            gameSocketManager.postAfterGameEnd(gameId)

        }

        // 게임 상세 결과 수신
        gameSocketManager.setOnEndGameAfter { data ->
            viewModelScope.launch {
                // 상세 결과를 저장하거나 처리 (MVP 정보 등)
                // data.optJSONObject("mvp") ...

                // 뉴스 화면으로 이동 이벤트 발생
                _uiEvent.emit(GamePlayUiEvent.NavigateToNews(gameId))

                viewModelScope.launch {
                    gameRepository.gameHardDelete(gameId) // TODO: 개발용 제거
                }
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

    fun setDefaultArea(context: Context) {
        viewModelScope.launch {
            val location = context.getSingleLocation()
            if (location != null) {
                locationRepository.updateCurrentLocation(location)
                locationRepository.createDefaultPolygon(location)
                locationRepository.setPrisonLocation(
                    LatLng(
                        location.latitude,
                        location.longitude
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        startService(GameActiveService.ACTION_STOP)
        gameSessionRepository.leaveGame()

        gameSocketManager.leaveGame()
        gameSocketManager.removeAllListeners()
    }


    private fun startService(action: String) {
        Intent(context, GameActiveService::class.java).also { intent ->
            intent.action = action

            // Android 8.0 (Oreo) 이상 대응
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent) // 이걸로 바꿔야 함!
            } else {
                context.startService(intent)
            }
        }
    }
}

sealed class GamePlayUiEvent {
    data class NavigateToNews(val gameId: Long) : GamePlayUiEvent()
    data class ThiefEscaped(val thiefId: Long, val thiefNickname: String) : GamePlayUiEvent()
}