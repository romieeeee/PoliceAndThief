package com.d104.pnt.ui.game.play

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRepository
import com.d104.pnt.data.repository.GameSessionEvent
import com.d104.pnt.data.repository.GameSessionRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.getSingleLocation
import com.d104.pnt.service.game.GameActiveService
import com.d104.pnt.util.StepSensorManager
import com.d104.pnt.util.socket.GameSocketManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    val userLocation = locationRepository.currentLocation
    val polygonPoints = locationRepository.polygonPoints
    val prisonLocation = locationRepository.prisonLocation

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

    private var myMemberId: Long = 0L
    private var warningJob: Job? = null

    init {
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
                    is GameSessionEvent.GameEnded -> { Timber.d("SessionEvent: GameEnded")}
                    is GameSessionEvent.ErrorOccurred -> {Timber.d("SessionEvent: ErrorOccurred - ${event.message}")}
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
                context.startForegroundService(intent) // ★ 이걸로 바꿔야 함!
            } else {
                context.startService(intent)
            }
        }
    }
}

sealed class GamePlayUiEvent {
    data class NavigateToNews(val gameId: Long) : GamePlayUiEvent()
}