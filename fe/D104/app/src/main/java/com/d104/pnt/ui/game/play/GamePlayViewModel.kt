package com.d104.pnt.ui.game.play

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.base.Constants
import com.d104.pnt.data.remote.model.response.BeepUseResponse
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameSessionEvent
import com.d104.pnt.data.repository.GameSessionRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.data.repository.WalkieRepository
import com.d104.pnt.domain.model.PlayerData
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.service.game.GameActiveService
import com.d104.pnt.util.StepSensorManager
import com.d104.pnt.util.getSingleLocation
import com.d104.pnt.util.socket.GameSocketManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class GamePlayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val authRepository: AuthRepository,
    private val gameSocketManager: GameSocketManager,
    savedStateHandle: SavedStateHandle,
    private val gameSessionRepository: GameSessionRepository,
    private val stepSensorManager: StepSensorManager,
    private val walkieRepository: WalkieRepository
) : ViewModel() {
    // UI 이벤트
    private val _uiEvent = MutableSharedFlow<GameSessionEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEvent = _uiEvent.asSharedFlow()

    // Beep 이벤트 (도둑 쪽에서만 화면이 소리 재생하도록 Screen에서 필터)
    private val _beepEvent = MutableSharedFlow<BeepUseResponse>(extraBufferCapacity = 16)
    val beepEvent = _beepEvent.asSharedFlow()

    // 게임 위치정보
    val userLocation = locationRepository.currentLocation
    val polygonPoints = locationRepository.polygonPoints
    val prisonLocation = locationRepository.prisonLocation

    // 게임 스탯 정보
    val gameId = gameSessionRepository.gameId
    val myMemberId = gameSessionRepository.myMemberId
    val myRole = gameSessionRepository.myRole
    // raw GPS -> repo 저장값
    val members = gameSessionRepository.members
    val memberLocation = gameSessionRepository.memberLocation
    val gameStatus = gameSessionRepository.gameStatus
    val missions = gameSessionRepository.missions
    val missionState = gameSessionRepository.missionState
    val missionFailReason = gameSessionRepository.missionFailReason
    val isOutOfBoundary = gameSessionRepository.isOutOfBoundary
    val thiefMembers = gameSessionRepository.thiefMembers
    val escapeQueue = gameSessionRepository.escapeQueue
    val isChief = gameSessionRepository.isChief

    private var warningJob: Job? = null
    private var pttHeartbeatJob: Job? = null
    private var radioTimeoutJob: Job? = null

    // ===== 무전기 =====
    val walkieConnected = walkieRepository.isConnected
    val walkieMicEnabled = walkieRepository.isMicEnabled
    val walkieParticipantCount = walkieRepository.participantCount
    val walkieState = gameSessionRepository.walkieState
    val isSomeoneTalking = gameSessionRepository.isSomeoneTalking
    val isTransmitting = gameSessionRepository.isTransmitting
    val talkingMemberId = gameSessionRepository.talkingMemberId

    // 경찰 헬기 스킬
    val helicopterState = gameSessionRepository.helicopterState
    val helicopterUsed = gameSessionRepository.helicopterUsed
    val helicopterButtonEnabled = gameSessionRepository.helicopterButtonEnabled

    init {
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

    fun initGame() {
        viewModelScope.launch {
            val token = authRepository.getAccessToken().first()
            if (token.isNotEmpty() && !gameSocketManager.isConnected()) {
                gameSocketManager.connect(token)
                while (!gameSocketManager.isConnected()) {
                    delay(100)
                }
            }
            delay(300)
            gameSocketManager.syncGameInfo()
        }
    }

    fun dequeEscape() {
        viewModelScope.launch {
            escapeQueue.value.drop(1)
        }
    }

    fun missionInit() {
        viewModelScope.launch {
            gameSessionRepository.missionInit()
        }
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

    fun connectWalkie() {
        viewModelScope.launch {
            gameSessionRepository.connectWalkie()
        }
    }

    fun disconnectWalkie() {
        viewModelScope.launch {
            gameSessionRepository.disconnectWalkie()
        }
    }

    fun useHelicopterSkill() {
        viewModelScope.launch {
            gameSessionRepository.useHelicopterSkill()
        }
    }

    fun startTalking() {
        viewModelScope.launch {
            gameSessionRepository.startTalking()
        }
    }

    fun stopTalking() {
        viewModelScope.launch {
            gameSessionRepository.stopTalking()
        }
    }

    fun manualLeaveGame() {
        viewModelScope.launch {
            Timber.d("🚪 유저가 직접 게임 종료를 선택함")
            startService(GameActiveService.ACTION_STOP)
            gameSessionRepository.leaveGame()
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameSessionRepository.disconnectWalkie()
        startService(GameActiveService.ACTION_STOP)
    }
}