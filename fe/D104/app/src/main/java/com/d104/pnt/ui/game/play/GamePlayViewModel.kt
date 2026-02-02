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
import com.d104.pnt.domain.model.PlayerData
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import timber.log.Timber
import java.time.Instant
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

    // Beep 이벤트
    private val _beepEvent = MutableSharedFlow<BeepUseResponse>(extraBufferCapacity = 16)
    val beepEvent = _beepEvent.asSharedFlow()

    val userLocation = locationRepository.currentLocation
    val polygonPoints = locationRepository.polygonPoints
    val prisonLocation = locationRepository.prisonLocation

    // raw GPS -> repo 저장값
    val playerLocations = locationRepository.playerLocations

    private val _allMembers = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())
    val allMembers: StateFlow<List<GameMemberSocketDto>> = _allMembers.asStateFlow()

    val gameStatus = gameSessionRepository.gameStatus
    val missions = gameSessionRepository.missions

    val thiefMembers = _allMembers
        .map { list -> list.filter { it.position.equals("THIEF", ignoreCase = true) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var myMemberId: Long = 0L
    private var warningJob: Job? = null

    // =========================
    // 🚁 Helicopter Skill State
    // =========================
    private val _helicopterState = MutableStateFlow(HelicopterUiState())
    val helicopterState = _helicopterState.asStateFlow()

    // CCTV로 잡힌 도둑(평소 1명 공개용)
    private val _cctvThiefId = MutableStateFlow<Long?>(null)
    val cctvThiefId = _cctvThiefId.asStateFlow()

    // 경찰 미니맵에서 표시할 플레이어들(경찰 + (CCTV 1명 도둑 or 도둑 전체))
    private val _minimapPlayers = MutableStateFlow<List<PlayerData>>(emptyList())
    val minimapPlayers: StateFlow<List<PlayerData>> = _minimapPlayers.asStateFlow()

    init {
        fetchMyId()
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
                    is GameSessionEvent.GameStarted -> startService(GameActiveService.ACTION_START)
                    is GameSessionEvent.GameEnded -> Timber.d("SessionEvent: GameEnded")
                    is GameSessionEvent.ErrorOccurred -> Timber.d("SessionEvent: ErrorOccurred - ${event.message}")
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
                while (!gameSocketManager.isConnected()) delay(100)
            }

            gameSocketManager.joinGame(gameId)
            delay(300)
            gameSocketManager.syncGameInfo()
        }
    }

    private fun setupSocketListeners() {
        // beep 수신
        gameSocketManager.setOnBeepReceived { policeId, thiefId, distance ->
            _beepEvent.tryEmit(BeepUseResponse(policeId, thiefId, distance))
            Timber.d("📢 beep 수신: policeId=$policeId, thiefId=$thiefId, distance=$distance")
        }

        /**
         * ⚠️ 여기 중요
         * 지금 GameSocketManager의 setOnGpsReceived 시그니처가 (sec, JSONArray)인지,
         * (sec, JSONArray, cctvThiefId, skillUsedAt)인지 프로젝트 코드랑 맞춰야 해요.
         *
         * 너가 지금 ViewModel에서 4개 파라미터 받는 형태로 쓰고 있으니,
         * GameSocketManager도 그 형태로 수정되어 있어야 정상 컴파일 됩니다.
         */
        gameSocketManager.setOnGpsReceived { sec, locations, cctvThiefId, skillUsedAt ->
            val list = parsePlayersFromGps(locations)
            locationRepository.updatePlayerLocation(list)

            _cctvThiefId.value = cctvThiefId
            updateHelicopterStateFromSkillUsedAt(skillUsedAt)

            recomputeMinimapPlayers(raw = list)
        }

        // 게임 정보 동기화
        gameSocketManager.setOnGameInfoSynced { data ->
            viewModelScope.launch {
                try {
                    val membersArray = data.optJSONArray("members") ?: return@launch
                    val newMembers = mutableListOf<GameMemberSocketDto>()
                    for (i in 0 until membersArray.length()) {
                        val memberJson = membersArray.getJSONObject(i)
                        newMembers.add(GameMemberSocketDto.fromJson(memberJson))
                    }
                    updateMembersList(newMembers)
                    Timber.d("GamePlayViewModel: 전체 멤버 동기화 완료 (${newMembers.size}명)")
                } catch (e: Exception) {
                    Timber.e(e, "GamePlayViewModel: 게임 정보 파싱 실패")
                }
            }
        }

        // 상태 변경
        gameSocketManager.setOnMemberStatusChanged { _, thiefId, status, _ ->
            viewModelScope.launch {
                val currentList = _allMembers.value.toMutableList()
                val idx = currentList.indexOfFirst { it.memberId == thiefId }
                if (idx != -1) {
                    val old = currentList[idx]
                    currentList[idx] = old.copy(rawStatus = status)
                    updateMembersList(currentList)
                    Timber.d("GamePlayViewModel: 도둑($thiefId) 상태 변경 -> $status")
                }
            }
        }

        // 게임 종료
        gameSocketManager.setOnGameEnded { winnerPosition, _ ->
            Timber.d("🏁 게임 종료 수신: $winnerPosition 승리")
            gameSocketManager.postAfterGameEnd(gameId)
        }

        // 게임 종료 상세
        gameSocketManager.setOnEndGameAfter {
            viewModelScope.launch {
                _uiEvent.emit(GamePlayUiEvent.NavigateToNews(gameId))
                viewModelScope.launch { gameRepository.gameHardDelete(gameId) } // TODO: 개발용
            }
        }
    }

    private fun updateMembersList(newList: List<GameMemberSocketDto>) {
        _allMembers.value = newList
        Timber.d("👥 멤버 리스트 갱신: ${newList.size}명 / thief=${newList.count { it.position.equals("THIEF", true) }}")
    }

    fun setDefaultArea(context: Context) {
        viewModelScope.launch {
            val location = context.getSingleLocation()
            if (location != null) {
                locationRepository.updateCurrentLocation(location)
                locationRepository.createDefaultPolygon(location)
                locationRepository.setPrisonLocation(LatLng(location.latitude, location.longitude))
            }
        }
    }

    // =========================
    // 🚁 Helicopter State Logic
    // =========================
    private fun updateHelicopterStateFromSkillUsedAt(skillUsedAtRaw: String?) {
        if (skillUsedAtRaw.isNullOrBlank()) {
            _helicopterState.value = HelicopterUiState()
            return
        }

        val usedAt = try {
            Instant.parse(skillUsedAtRaw)
        } catch (e: Exception) {
            Timber.e(e, "🚁 skillUsedAt 파싱 실패: $skillUsedAtRaw")
            return
        }

        val now = Instant.now()
        val notifyEnd = usedAt.plusSeconds(5)
        val revealEnd = usedAt.plusSeconds(15)

        val phase = when {
            now.isBefore(notifyEnd) -> HelicopterPhase.NOTIFY
            now.isBefore(revealEnd) -> HelicopterPhase.REVEAL
            else -> HelicopterPhase.IDLE
        }

        val remainingMs = when (phase) {
            HelicopterPhase.NOTIFY -> (notifyEnd.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0)
            HelicopterPhase.REVEAL -> (revealEnd.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0)
            HelicopterPhase.IDLE -> 0L
        }

        _helicopterState.value = HelicopterUiState(
            usedAt = if (phase == HelicopterPhase.IDLE) null else usedAt,
            phase = phase,
            notifyEndsAt = if (phase != HelicopterPhase.IDLE) notifyEnd else null,
            revealEndsAt = if (phase != HelicopterPhase.IDLE) revealEnd else null,
            remainingMs = remainingMs
        )
    }

    private fun parsePlayersFromGps(locations: JSONArray): List<PlayerData> {
        val result = ArrayList<PlayerData>(locations.length())
        for (i in 0 until locations.length()) {
            val o = locations.optJSONObject(i) ?: continue

            val memberId = o.optLong("memberId", 0L)
            val gameIdRaw = o.opt("gameId")
            val parsedGameId = when (gameIdRaw) {
                is Number -> gameIdRaw.toLong()
                is String -> gameIdRaw.toLongOrNull() ?: 0L
                else -> 0L
            }

            val lat = o.optDouble("lat", 0.0)
            val lng = o.optDouble("lng", 0.0)
            val walk = o.optInt("walk", 0)
            val longestSurvived = o.optInt("longestSurvived", 0)
            val position = o.optString("position", "")
            val status = o.optString("status", "")
            val penalty = o.optInt("penalty", 0)
            val timeStamp = o.optString("timestamp", "")

            result.add(
                PlayerData(
                    id = 0L,
                    gameId = parsedGameId,
                    memberId = memberId,
                    lat = lat,
                    lng = lng,
                    walk = walk,
                    longestSurvived = longestSurvived,
                    position = position,
                    status = status,
                    penalty = penalty,
                    timeStamp = timeStamp
                )
            )
        }
        return result
    }

    private fun recomputeMinimapPlayers(raw: List<PlayerData>) {
        val police = raw.filter {
            it.position.equals("POLICE", ignoreCase = true) && it.memberId != myMemberId
        }

        val thieves = raw.filter { it.position.equals("THIEF", ignoreCase = true) }

        val visibleThieves = when (helicopterState.value.phase) {
            HelicopterPhase.REVEAL -> thieves
            else -> {
                val targetId = _cctvThiefId.value
                if (targetId == null) emptyList()
                else thieves.filter { it.memberId == targetId }
            }
        }

        _minimapPlayers.value = police + visibleThieves
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

sealed class GamePlayUiEvent {
    data class NavigateToNews(val gameId: Long) : GamePlayUiEvent()
}

data class HelicopterUiState(
    val usedAt: Instant? = null,
    val phase: HelicopterPhase = HelicopterPhase.IDLE,
    val notifyEndsAt: Instant? = null,
    val revealEndsAt: Instant? = null,
    val remainingMs: Long = 0L
)

enum class HelicopterPhase {
    IDLE,
    NOTIFY,
    REVEAL
}
