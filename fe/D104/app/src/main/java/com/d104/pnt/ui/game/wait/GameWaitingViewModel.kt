package com.d104.pnt.ui.game.wait

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRoomRepository
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.navigation.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.domain.model.DraggableLatLng
import com.google.android.gms.maps.model.LatLng

@HiltViewModel
class GameWaitingViewModel @Inject constructor(
    private val gameRoomRepository: GameRoomRepository,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomId: Long = savedStateHandle.get<Long>(NavArgs.ROOM_ID) ?: 0L

    // 내 Member ID
    private val _myMemberId = MutableStateFlow(0L)
    val myMemberId: StateFlow<Long> = _myMemberId.asStateFlow()

    // 참가자 리스트
    private val _players = MutableStateFlow<List<WaitingPlayer>>(emptyList())
    val players: StateFlow<List<WaitingPlayer>> = _players.asStateFlow()

    // 방 정보
    private val _roomInfo = MutableStateFlow(GameRoomInfoState())
    val roomInfo: StateFlow<GameRoomInfoState> = _roomInfo.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _isMeReady = MutableStateFlow(false)
    val isMeReady: StateFlow<Boolean> = _isMeReady.asStateFlow()

    // 로딩/에러 상태 관리
    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<GameWaitingUiEvent>()
    val uiEvent: SharedFlow<GameWaitingUiEvent> = _uiEvent.asSharedFlow()

    // 역할 변경 중 상태
    private val _changingRoleMemberIds = MutableStateFlow<Set<Long>>(emptySet())

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.getMemberId().collect { id ->
                if (id != 0L) {
                    _myMemberId.value = id
                    Timber.d("GameWaitingViewModel: 내 ID 로드 성공 -> $id")
                }
            }
        }

        loadRoomSettings()
    }

    // 방 설정 조회
    private fun loadRoomSettings() {
        viewModelScope.launch {
            when (val result = gameRoomRepository.getRoomSettings(roomId)) {
                is BaseResult.Success -> {
                    val data = result.data

                    val cachedRoom = gameRoomRepository.getCurrentGameRoom().value

                    val finalRoomCode = if (!data.roomCode.isNullOrEmpty()) {
                        data.roomCode
                    } else {
                        cachedRoom?.roomCode ?: ""
                    }

                    val prisonLocation = Location(data.prisonLat, data.prisonLng)
                    val cachedPolygon = cachedRoom?.polygon?.map {
                        Location(it.latitude, it.longitude)
                    } ?: emptyList()

                    _roomInfo.value = _roomInfo.value.copy(
                        roomCode = finalRoomCode,

                        maxCount = data.playerCount,
                        timeLimit = data.timeLimit,
                        missionCount = data.missionCount,
                        cctvCycle = data.cctvInterval,
                        policeCount = data.policeCount,
                        thiefCount = data.thiefCount,

                        prison = prisonLocation,
                        polygon = cachedPolygon
                    )
                    Timber.d("RoomSettings: 설정 로드 완료 (Code: $finalRoomCode, Polygon: ${cachedPolygon.size})")
                }
                is BaseResult.Error -> {
                    Timber.e("RoomSettings: 로드 실패 ${result.error.message}")
                }
            }
        }
    }

    // 멤버 리스트 조회
    private suspend fun fetchMembers() {
        when (val result = gameRoomRepository.getRoomMembers(roomId)) {
            is BaseResult.Success -> {
                val items = result.data.items
                val currentMemberId = myMemberId.value
                val isMeInList = items.any { it.memberId == currentMemberId }

                if (currentMemberId != 0L && !isMeInList) {
                    pollingJob?.cancel()
                    _uiEvent.emit(GameWaitingUiEvent.NavigateToHome(message = "강퇴되었습니다!"))
                    return
                }

                val uiPlayers = items.map { item ->
                    if (item.host) {
                        Timber.d("CheckHost: 방장 발견! ID=${item.memberId}, 닉네임=${item.nickname}")
                    }

                    if (item.memberId == currentMemberId) {
                        if (_isHost.value != item.host) {
                            _isHost.value = item.host
                            Timber.d("CheckHost: 내 방장 권한 변경됨 -> ${item.host}")
                        }

                        if (_isMeReady.value != item.ready) {
                            _isMeReady.value = item.ready
                        }
                    }

                    WaitingPlayer(
                        id = item.memberId,
                        nickname = item.nickname,

                        role = GameRole.fromName(item.preferPosition),

                        isReady = item.ready,

                        profileUrl = item.avatarUrl,

                        isChangingRole = _changingRoleMemberIds.value.contains(item.memberId)
                    )
                }
                _players.value = uiPlayers
            }
            is BaseResult.Error -> {
                Timber.e("멤버 조회 실패: ${result.error.message}")

                pollingJob?.cancel()
                _uiEvent.emit(GameWaitingUiEvent.NavigateToHome(message = "강퇴되었거나 방 연결이 끊어졌습니다."))
            }
        }
    }
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchMembers()
                delay(3000)
            }
        }
    }

    // 준비
    fun toggleReady() {
        viewModelScope.launch {
            val nextState = !_isMeReady.value
            _isMeReady.value = nextState // UI 선반영

            val result = gameRoomRepository.toggleReady(roomId, nextState)
            if (result is BaseResult.Success) {
                fetchMembers()
            } else {
                _isMeReady.value = !nextState // 롤백
                _uiState.value = UiState.Error("준비 상태 변경 실패")
            }
        }
    }

    // 역할 변경
    fun changeRole() {
        viewModelScope.launch {
            val currentMemberId = myMemberId.value
            if (currentMemberId == 0L) return@launch

            val myPlayer = _players.value.find { it.id == currentMemberId } ?: return@launch
            val nextRole = if (myPlayer.role == GameRole.POLICE) "THIEF" else "POLICE"

            _changingRoleMemberIds.value += currentMemberId // 로컬 UI 변경

            val result = gameRoomRepository.changePosition(roomId, nextRole)

            _changingRoleMemberIds.value -= currentMemberId // 로컬 UI 해제

            if (result is BaseResult.Success) {
                fetchMembers()
            } else {
                _uiState.value = UiState.Error("역할 변경 실패")
            }
        }
    }

    // 게임 시작
    fun startGame() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = gameRoomRepository.startGame(roomId)) {
                is BaseResult.Success -> {
                    Timber.d("게임 시작 성공")
                    _uiState.value = UiState.Success(Unit) // 화면 이동 트리거용
                }
                is BaseResult.Error -> {
                    _uiState.value = UiState.Error(result.error.message)
                }
            }
        }
    }

    // 방 설정 변경
    fun updateRoomSettings(
        maxCount: Int,
        timeLimit: Int,
        missionCount: Int,
        cctvCycle: Int,
        policeCount: Int,
        prisonLocation: Location,
        polygonPoints: List<Location>
    ) {
        viewModelScope.launch {
            val current = _roomInfo.value

            val safeMaxCount = maxCount.coerceAtLeast(5)
            val safePoliceCount = policeCount.coerceIn(1, safeMaxCount - 1)
            val thiefCount = safeMaxCount - safePoliceCount

            val result = gameRoomRepository.updateRoomSettings(
                roomId = roomId,
                playerCount = safeMaxCount,
                timeLimit = timeLimit,
                cctvInterval = cctvCycle,
                policeCount = safePoliceCount,
                thiefCount = thiefCount,
                missionCount = missionCount,
                prison = prisonLocation,
                polygon = polygonPoints
            )

            when (result) {
                is BaseResult.Success -> {
                    Timber.d("RoomSettings: 서버 설정 변경 성공")
                    _roomInfo.value = current.copy(
                        maxCount = safeMaxCount,
                        timeLimit = timeLimit,
                        missionCount = missionCount,
                        cctvCycle = cctvCycle,
                        policeCount = safePoliceCount,
                        thiefCount = thiefCount,
                        prison = prisonLocation,
                        polygon = polygonPoints
                    )
                }
                is BaseResult.Error -> {
                    Timber.e("RoomSettings: 변경 실패 ${result.error.message}")
                    _uiState.emit(UiState.Error("설정 변경 실패: ${result.error.message}"))
                }
            }
        }
    }

    // 방 나가기
    fun leaveRoom() {
        viewModelScope.launch {
            pollingJob?.cancel()

            val result = gameRoomRepository.leaveRoom(roomId)

            when (result) {
                is BaseResult.Success -> {
                    _uiEvent.emit(GameWaitingUiEvent.NavigateToHome())
                }
                is BaseResult.Error -> {
                    _uiEvent.emit(GameWaitingUiEvent.NavigateToHome())
                }
            }
        }
    }

    fun kickPlayer(targetMemberId: Long, reason: String) {
        viewModelScope.launch {
            val result = gameRoomRepository.kickPlayer(roomId, targetMemberId, reason)

            when (result) {
                is BaseResult.Success -> { }
                is BaseResult.Error -> {
                    _uiState.value = UiState.Error(message = result.error.message ?: "강퇴 실패")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    fun setInitialRole(role: GameRole) {
        viewModelScope.launch {
            val position = role.name

            Timber.d("초기 역할 설정 요청: $position")

            val result = gameRoomRepository.changePosition(roomId, position)

            if (result is BaseResult.Error) {
                Timber.e("초기 역할 설정 실패: ${result.error.message}")
            }

            fetchMembers()
            startPolling()
        }
    }

    fun resetToUndecided() {
        viewModelScope.launch {

            gameRoomRepository.changePosition(roomId, "UNDECIDED")
        }
    }

    fun setPolygonPoints(points: List<LatLng>) {
        locationRepository.setPolygonPoints(points)
    }

    fun setPrisonLocation(location: LatLng?) {
        locationRepository.setPrisonLocation(location)
    }

    fun deletePolygonPoint(targetList: MutableList<DraggableLatLng>, index: Int): Boolean {
        return locationRepository.deletePolygonPoint(targetList, index)
    }

    fun addPointToList(targetList: MutableList<DraggableLatLng>, newPoint: LatLng) {
        locationRepository.addPointToList(targetList, newPoint)
    }

}