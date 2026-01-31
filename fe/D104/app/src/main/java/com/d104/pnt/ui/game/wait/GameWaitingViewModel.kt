package com.d104.pnt.ui.game.wait

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRoomRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.domain.model.DraggableLatLng
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.domain.model.RoomInfoResponse
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.socket.RoomSocketManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GameWaitingViewModel @Inject constructor(
    private val gameRoomRepository: GameRoomRepository,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val roomSocketManager: RoomSocketManager,
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


    init {
        roomSocketManager.currentRoomId = roomId
        setupRoomCallbacks()

        viewModelScope.launch {
            authRepository.getMemberId().collect { id ->
                if (id != 0L) {
                    _myMemberId.value = id
                    syncData()
                }
            }
        }

        loadRoomSettings()
    }

    private fun syncData() {
        viewModelScope.launch {
            while (!roomSocketManager.isConnected()) {
                delay(200)
            }

            roomSocketManager.joinRoom(roomId) { success, _ ->
                if (success) {
                    roomSocketManager.requestRoomInfo(roomId)
                    roomSocketManager.requestReadyInfo(roomId)
                }
            }
        }
    }

    fun resetToUndecided() {
        viewModelScope.launch {
            // 방을 나가지 않고(leaveRoom X), 역할만 ANY로 바꿈
            val result = gameRoomRepository.changePosition(roomId, "ANY")
            if (result is BaseResult.Success) {
                roomSocketManager.updatePosition("ANY")
            }
        }
    }

    /**
     * Socket 콜백 설정
     */
    private fun setupRoomCallbacks() {
        // 전체 방 정보 수신
        roomSocketManager.setOnFullRoomInfoReceived { data ->
            Timber.d("📥 [PRIMARY] 전체 방 정보 수신 - UI 업데이트")
            parseFullRoomInfo(data)
            _uiState.value = UiState.Idle
        }

        // 방 설정 업데이트
        roomSocketManager.setOnRoomInfoUpdated { data ->
            Timber.d("📥 방 설정 업데이트")
            parseRoomSettings(data)
        }

        // Ready 상태 업데이트
        roomSocketManager.setOnReadyUpdated { roomId, memberId, isReady ->
            Timber.d("📥 Ready 업데이트: memberId=$memberId, ready=$isReady")
            updatePlayerReady(memberId, isReady)
        }

        // 포지션 업데이트
        roomSocketManager.setOnPositionUpdated { roomId, memberId, position ->
            Timber.d("📥 포지션 업데이트: memberId=$memberId, position=$position")
            updatePlayerPosition(memberId, position)
        }

        // 멤버 강퇴
        roomSocketManager.setOnMemberKicked { memberId ->
//            if (memberId == 0L) {
//                // 💡 1. 서버가 ID를 안 준다면? 일단 방 정보를 다시 요청해서 리스트를 갱신해봅니다.
//                Timber.w("⚠️ 강퇴 ID가 없어서 방 정보를 재요청합니다.")
//                roomSocketManager.requestRoomInfo(roomId)
//
//                // 💡 2. 그리고 내가 여전히 이 방 멤버인지 확인하는 로직이 필요합니다.
//                // (이건 서버가 나를 강퇴했다면 방 정보 members 리스트에 내가 없을 테니까요)
//            } else if (memberId == _myMemberId.value) {
//                viewModelScope.launch {
//                    _uiEvent.emit(GameWaitingUiEvent.NavigateToHome("방에서 강퇴되었습니다."))
//                }
//            } else {
//                removePlayer(memberId)
//            }
            Timber.d("📥 멤버 강퇴: $memberId")
            if (memberId == _myMemberId.value) {
                viewModelScope.launch {
                    _uiEvent.emit(GameWaitingUiEvent.NavigateToHome("방에서 강퇴되었습니다."))
                }
            } else {
                removePlayer(memberId)
            }
        }

        // 멤버 퇴장
        roomSocketManager.setOnMemberLeft { memberId ->
            Timber.d("📥 멤버 퇴장: $memberId")
            removePlayer(memberId)
        }
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


    /**
     * 준비 상태 변경 (HTTP → Socket)
     */
    fun toggleReady() {
        viewModelScope.launch {
            val nextState = !_isMeReady.value

            when (gameRoomRepository.toggleReady(roomId, nextState)) {
                is BaseResult.Success -> {
                    roomSocketManager.updateReady(roomId, nextState)
                }

                is BaseResult.Error -> {
                    _uiState.value = UiState.Error("준비 상태 변경 실패")
                }
            }
        }
    }

    /**
     * 전체 방 정보 파싱
     */
    private fun parseFullRoomInfo(data: RoomInfoResponse) {
        try {
            // 1. 소켓 데이터에서 방장 ID를 미리 가져옵니다.
            val hostId = data.room.hostMemberId
            val myId = _myMemberId.value
            val changingMemberIds = _changingRoleMemberIds.value

            // 내 강퇴 여부 체크 (팀원 로직 반영)
            val isMeInList = data.members.any { it.memberId == myId }
            if (myId != 0L && !isMeInList) {
                viewModelScope.launch { _uiEvent.emit(GameWaitingUiEvent.NavigateToHome("강퇴되었습니다!")) }
                return
            }

            _players.value = data.members.distinctBy { it.memberId }.map { member ->
                val isMe = member.memberId == myId

                // 🔥 핵심: 팀원들이 썼던 item.host 대신 이걸 씁니다!
                val isThisMemberHost = (member.memberId == hostId)

                // 포지션 결정 (팀원 로직: given 우선)
                val displayRoleString = if (!member.givenPosition.isNullOrEmpty() && member.givenPosition != "UNDECIDED") {
                    member.givenPosition
                } else {
                    member.preferPosition
                }

                // [팀원 로직 반영] 역할 변경 중일 때 방장 ready 상태 조정
                val isChangingRole = changingMemberIds.contains(member.memberId)
                val adjustedReady = if (isThisMemberHost) { // 여기서 위에서 만든 변수 사용
                    !isChangingRole
                } else {
                    member.ready
                }

                // 내 상태 업데이트
                if (isMe) {
                    _isHost.value = isThisMemberHost
                    _isMeReady.value = adjustedReady
                }

                WaitingPlayer(
                    id = member.memberId,
                    nickname = member.memberDetail.profile.nickname,
                    role = GameRole.fromName(displayRoleString ?: "ANY"),
                    isReady = adjustedReady,
                    profileUrl = member.memberDetail.profile.avatarUrl,
                    isChangingRole = isChangingRole,
                    isHost = isThisMemberHost // UI에도 방장 여부 전달
                )
            }

            // 새로 받은 명단에 내 ID가 없고, 내 ID가 0이 아닐 때 (방에 들어가 있는 상태였을 때)
            if (myId != 0L && _players.value.none { it.id == myId }) {
                Timber.w("🚨 내 ID가 서버 명단에 없습니다. 강퇴된 것으로 판단하여 홈으로 이동합니다.")
                viewModelScope.launch {
                    _uiEvent.emit(GameWaitingUiEvent.NavigateToHome("방에서 강퇴되었습니다."))
                }
                return // 이후 로직 중단
            }
            Timber.d("✅ UI 업데이트 완료: players=${_players.value.size}, isHost=${_isHost.value}, myReady=${_isMeReady.value}")
        } catch (e: Exception) {
            Timber.e(e, "❌ 방 정보 파싱 실패")
            _uiState.value = UiState.Error("방 정보 파싱 실패")
        }
    }

    /**
     * 방 설정 파싱
     */
    private fun parseRoomSettings(data: JSONObject) {
        try {
            _roomInfo.value = _roomInfo.value.copy(
                maxCount = data.getInt("playerCount"),
                policeCount = data.getInt("policeCount"),
                thiefCount = data.getInt("thiefCount"),
                timeLimit = data.getInt("timeLimit") / 60,
                prison = Location(
                    lat = data.getJSONObject("prison").getDouble("lat"),
                    lng = data.getJSONObject("prison").getDouble("lng")
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "방 설정 파싱 실패")
        }
    }

    /**
     * 플레이어 Ready 상태 업데이트
     */
    private fun updatePlayerReady(memberId: Long, isReady: Boolean) {
        // 💡 새로운 리스트를 만들어 할당해야 UI가 확실히 바뀝니다.
        val currentPlayers = _players.value
        _players.value = currentPlayers.map { player ->
            if (player.id == memberId) {
                // 내 상태면 _isMeReady도 같이 업데이트
                if (memberId == _myMemberId.value) {
                    _isMeReady.value = isReady
                }
                player.copy(isReady = isReady)
            } else {
                player
            }
        }
    }

    /**
     * 플레이어 포지션 업데이트
     */
    private fun updatePlayerPosition(memberId: Long, position: String) {
        _players.value = _players.value.map { player ->
            if (player.id == memberId) {
                player.copy(
                    role = when (position) {
                        "POLICE" -> GameRole.POLICE
                        "THIEF" -> GameRole.THIEF
                        else -> GameRole.ANY
                    },
                    isChangingRole = false
                )
            } else {
                player
            }
        }
    }

    /**
     * 게임 시작
     */
    fun startGame() {
        if (!_isHost.value) return

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            when (val result = gameRoomRepository.startGame(roomId)) {
                is BaseResult.Success -> {
                    Timber.d("✅ 게임 시작 성공")
                    _uiState.value = UiState.Success(Unit)
                }

                is BaseResult.Error -> {
                    Timber.e("❌ 게임 시작 실패")
                    _uiState.value = UiState.Error(result.error.message)
                }
            }
        }
    }

    /**
     * 플레이어 제거
     */
    private fun removePlayer(memberId: Long) {
        _players.value = _players.value.filter { it.id != memberId }.toList()

        // 로그로 현재 남은 인원 확인
        Timber.d("👤 플레이어 제거 완료: $memberId, 남은 인원: ${_players.value.size}")
    }


    /**
     * 방 설정 변경 (HTTP → Socket)
     */
    fun updateRoomSettings(
        maxCount: Int,
        timeLimit: Int,
        missionCount: Int,
        cctvCycle: Int,
        policeCount: Int,
        prisonLocation: Location,
        polygonPoints: List<Location>
    ) {
        if (!_isHost.value) return

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
                    roomSocketManager.updateRoomInfo(
                        playerCount = safeMaxCount,
                        timeLimit = timeLimit,
                        policeCount = safePoliceCount,
                        thiefCount = thiefCount,
                        cctvInterval = cctvCycle,
                        missionCount = missionCount,
                        prison = current.prison,
                        polygon = current.polygon
                    )
                }

                is BaseResult.Error -> {
                    Timber.e("RoomSettings: 변경 실패 ${result.error.message}")
                    _uiState.emit(UiState.Error("설정 변경 실패: ${result.error.message}"))
                }
            }
        }
    }

    /**
     * 강퇴
     */
    fun kickPlayer(targetMemberId: Long, reason: String) {
        if (!_isHost.value) return

        roomSocketManager.kickMember(targetMemberId, reason) { kickedId ->
            removePlayer(kickedId)
            roomSocketManager.requestRoomInfo(roomId)
        }
    }

    /**
     * 방 나가기
     */
    fun leaveRoom() {
        viewModelScope.launch {
            roomSocketManager.leaveRoom()

            roomSocketManager.disconnect()

            gameRoomRepository.leaveRoom(roomId)

            _uiEvent.emit(GameWaitingUiEvent.NavigateToHome())
        }
    }

    fun setInitialRole(role: GameRole) {
        viewModelScope.launch {
            val position = role.name

            val result = gameRoomRepository.changePosition(roomId, position)

            if (result is BaseResult.Success) {
                // 연결된 상태일 때만 소켓 신호를 보냅니다.
                while (!roomSocketManager.isConnected()) {
                    delay(200) // 소켓 연결 대기
                }
                roomSocketManager.updatePosition(position)
                Timber.d("📤 [Socket] 초기 역할 설정 완료: $position")
            }
        }
    }

    // 방장 위임하기
    fun delegateHost(targetMemberId: Long) {
        viewModelScope.launch {
            Timber.d("방장 위임 요청: targetMemberId=$targetMemberId")

            // TODO: 나중에 API 연결 시 주석 해제
            // val result = gameRoomRepository.delegateHost(roomId, targetMemberId)

            delay(100)
//            fetchMembers()
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