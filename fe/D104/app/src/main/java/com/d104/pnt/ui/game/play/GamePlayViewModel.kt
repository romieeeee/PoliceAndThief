package com.d104.pnt.ui.game.play

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
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
    private val _uiEvent = MutableSharedFlow<GameSessionEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val uiEvent = _uiEvent.asSharedFlow()
    val isOutOfBoundary = gameSessionRepository.isOutOfBoundary

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

    private var myMemberId: Long = 0L
    private var warningJob: Job? = null

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