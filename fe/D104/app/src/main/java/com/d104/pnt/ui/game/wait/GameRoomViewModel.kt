package com.d104.pnt.ui.game.wait

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRoomRepository
import com.d104.pnt.data.repository.GameSessionRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.domain.model.DraggableLatLng
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.domain.model.GameRoomInfoState
import com.d104.pnt.domain.model.GameRoomUiEvent
import com.d104.pnt.domain.model.RoomInfoResponse
import com.d104.pnt.domain.model.WaitingPlayer
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GameRoomViewModel @Inject constructor(
    private val gameRoomRepository: GameRoomRepository,
    private val authRepository: AuthRepository,
    private val locationRepository: LocationRepository,
    private val roomSocketManager: RoomSocketManager,
    private val gameSessionRepository: GameSessionRepository,
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

    private val _uiEvent = MutableSharedFlow<GameRoomUiEvent>()
    val uiEvent: SharedFlow<GameRoomUiEvent> = _uiEvent.asSharedFlow()

    // 역할 변경 중 상태
    private val _changingRoleMemberIds = MutableStateFlow<Set<Long>>(emptySet())

    private var isJoined = false // 플래그 추가

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
        if (isJoined) return // 이미 가입 절차 중이면 무시
        isJoined = true

        viewModelScope.launch {
            val token = authRepository.getAccessToken().first()

            if (token.isNotEmpty()) {
                roomSocketManager.currentRoomId = roomId
                roomSocketManager.connect(token)

                var retryCount = 0
                while (!roomSocketManager.isConnected() && retryCount < 50) {
                    delay(200)
                    retryCount++
                }

                if (roomSocketManager.isConnected()) {
                    roomSocketManager.joinRoom(roomId) { success, _ ->
                        if (success) {
                            Timber.d("🌐 소켓 연결 및 방 입장 완료")
                        }
                    }
                } else {
                    Timber.e("❌ 소켓 연결 실패")
                    _uiState.value = UiState.Error("소켓 연결에 실패했습니다.")
                }
            }
        }
    }

    fun resetToUndecided() {
        viewModelScope.launch {
            // 방을 나가지 않고, 역할만 ANY로 바꿈
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
            Timber.d("📥 멤버 강퇴: $memberId")
            if (memberId == _myMemberId.value) {
                viewModelScope.launch {
                    _uiEvent.emit(GameRoomUiEvent.NavigateToHome("방에서 강퇴되었습니다."))
                }
            } else {
                // 다른 사람이 강퇴당함
                removePlayer(memberId)
            }
        }

        roomSocketManager.setOnMemberLeft { memberId ->
            Timber.d("📥 멤버 퇴장: $memberId")
            if (memberId == _myMemberId.value) {
                viewModelScope.launch {
                    _uiEvent.emit(GameRoomUiEvent.NavigateToHome("연결이 종료되었습니다."))
                }
            } else {
                removePlayer(memberId)
                roomSocketManager.requestRoomInfo(roomId)
            }
        }

        // 토큰 갱신
        roomSocketManager.setOnTokenUpdated { newToken ->
            viewModelScope.launch {
                val refreshToken = authRepository.getRefreshToken().first()
                authRepository.refreshTokens(newToken, refreshToken)
                roomSocketManager.updateAccessToken(newToken)
                Timber.d("✅ 토큰 갱신 및 동기화 완료")
            }
        }

        // 방장 위임
        roomSocketManager.setOnOwnerDelegated { data ->
            val code = data.optInt("code", 200)
            if (code == 200) {
                roomSocketManager.requestRoomInfo(roomId)
            } else {
                val msg = data.optString("message", "위임 실패")
                _uiState.value = UiState.Error(msg)
            }
        }

        // 게임 시작: 네임스페이스 교체 및 화면 이동
        roomSocketManager.setOnGameStarted { data ->
            viewModelScope.launch {
                try {
                    val currentCode = _roomInfo.value.roomCode
                    gameSessionRepository.setRoomCode(currentCode)

                    // 경찰청장(chiefMemberId) 저장 (0이면 null 처리)
                    val chiefId = data.optLong("chiefMemberId", 0L).let { if (it == 0L) null else it }
                    gameSessionRepository.setChiefMemberId(chiefId)
                    Timber.d("👮‍♂️ chiefMemberId 저장: $chiefId")

                    val membersArray = data.optJSONArray("members") ?: return@launch
                    val myId = _myMemberId.value
                    var myFinalRole = "ANY" // 기본값

                    for (i in 0 until membersArray.length()) {
                        val member = membersArray.getJSONObject(i)
                        if (member.optLong("memberId") == myId) {
                            myFinalRole = member.optString("givenPosition", "ANY")
                            break
                        }
                    }

                    Timber.d("🎮 최종 역할 확정: $myFinalRole (ID: $myId)")

                    // 결정된 정보 저장
                    gameSessionRepository.setFinalRole(myFinalRole)
                    gameSessionRepository.setMemberId(myId)
                    gameSessionRepository.setCctvInterval(roomInfo.value.cctvCycle)
                    locationRepository.setPrisonLocation(LatLng(_roomInfo.value.prison!!.lat, _roomInfo.value.prison!!.lng))
                    locationRepository.setPolygonPoints(_roomInfo.value.polygon!!.map { LatLng(it.lat, it.lng) })


                    delay(300)
                    roomSocketManager.disconnect()

                    _uiEvent.emit(GameRoomUiEvent.NavigateToGame(roomId, myFinalRole))

                } catch (e: Exception) {
                    Timber.e(e, "❌ 게임 시작 데이터 파싱 실패")
                }
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
//                    val polygon = data.boundaryGeo.coordinates.map { Location(it[0][1], it[0][1]) }

                    _roomInfo.value = _roomInfo.value.copy(
                        roomCode = finalRoomCode,

                        maxCount = data.playerCount,
                        timeLimit = data.timeLimit,
                        missionCount = data.missionCount,
                        cctvCycle = data.cctvInterval,
                        policeCount = data.policeCount,
                        thiefCount = data.thiefCount,

                        prison = prisonLocation,
                        polygon = _roomInfo.value.polygon
                    )
                    Timber.d("RoomSettings: 설정 로드 완료 (Code: $finalRoomCode, Polygon: ${_roomInfo.value.polygon})")
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
                viewModelScope.launch { _uiEvent.emit(GameRoomUiEvent.NavigateToHome("강퇴되었습니다!")) }
                return
            }

            _players.value = data.members.distinctBy { it.memberId }.map { member ->
                val isMe = member.memberId == myId

                val isThisMemberHost = (member.memberId == hostId)

                // 포지션 결정 (팀원 로직: given 우선)
                val displayRoleString =
                    if (!member.givenPosition.isNullOrEmpty() && member.givenPosition != "ANY") {
                        member.givenPosition
                    } else {
                        member.preferPosition
                    }

                val isChangingRole = changingMemberIds.contains(member.memberId)
                val adjustedReady = if (isThisMemberHost) {
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

            if (myId != 0L && _players.value.none { it.id == myId }) {
                viewModelScope.launch {
                    _uiEvent.emit(GameRoomUiEvent.NavigateToHome("방에서 강퇴되었습니다."))
                }
                return // 이후 로직 중단
            }
            Timber.d("전체 방정보 ${data}")

            _roomInfo.value = _roomInfo.value.copy(
                roomCode = data.room.roomCode,

                maxCount = data.roomSetting.playerCount,
                timeLimit = data.roomSetting.timeLimit,
                missionCount = _roomInfo.value.missionCount,
                cctvCycle = _roomInfo.value.cctvCycle,
                policeCount = data.roomSetting.policeCount,
                thiefCount = data.roomSetting.thiefCount,

                prison = Location(data.roomSetting.prisonLat, data.roomSetting.prisonLng),
                polygon = data.roomSetting.boundaryGeo.coordinates[0].map { Location(it[1], it[0]) }
            )

            val currentCode = _roomInfo.value.roomCode
            gameSessionRepository.setRoomCode(currentCode)

            Timber.d("감옥 위치 ${_roomInfo.value.prison}, 폴리곤 ${_roomInfo.value.polygon}")

        } catch (e: Exception) {
            Timber.e(e, "❌ 방 정보 파싱 실패 ${data}")
            _uiState.value = UiState.Error("방 정보 파싱 실패")
        }
    }

    /**
     * 방 설정 파싱
     */
    private fun parseRoomSettings(data: JSONObject) {
        try {
            if (data.has("code") && data.getInt("code") != 200) {
                val msg = data.optString("message", "설정 변경 실패")
                Timber.e("📥 서버 에러: $msg")
                return
            }

            val actualData = data.optJSONObject("data") ?: data

            _roomInfo.value.prison?.let { currentPrison ->
                val polygonArray = actualData.optJSONArray("polygon")
                val updatedPolygon = if (polygonArray != null) {
                    val list = mutableListOf<Location>()
                    for (i in 0 until polygonArray.length()) {
                        val obj = polygonArray.getJSONObject(i)
                        list.add(Location(obj.getDouble("lat"), obj.getDouble("lng")))
                    }
                    list
                } else {
                    _roomInfo.value.polygon
                }

                _roomInfo.value = _roomInfo.value.copy(
                    maxCount = actualData.optInt("playerCount", _roomInfo.value.maxCount),
                    policeCount = actualData.optInt("policeCount", _roomInfo.value.policeCount),
                    thiefCount = actualData.optInt("thiefCount", _roomInfo.value.thiefCount),
                    timeLimit = actualData.optInt("timeLimit", _roomInfo.value.timeLimit),
                    missionCount = actualData.optInt("missionCount", _roomInfo.value.missionCount),
                    cctvCycle = actualData.optInt("cctvInterval", _roomInfo.value.cctvCycle),
                    prison = Location(
                        lat = actualData.optDouble("prisonLat", currentPrison.lat),
                        lng = actualData.optDouble("prisonLng", currentPrison.lng)
                    ),
                    polygon = updatedPolygon
                )
            }


        } catch (e: Exception) {
            Timber.e(e, "❌ 방 설정 파싱 실패: 데이터 구조 확인 필요")
        }
    }

    /**
     * 플레이어 Ready 상태 업데이트
     */
    private fun updatePlayerReady(memberId: Long, isReady: Boolean) {
        val currentPlayers = _players.value
        _players.value = currentPlayers.map { player ->
            if (player.id == memberId) {
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
                    roomSocketManager.gameStart(roomId)

                    delay(500)

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
                    roomSocketManager.updateRoomInfo(
                        playerCount = safeMaxCount,
                        timeLimit = timeLimit,
                        policeCount = safePoliceCount,
                        thiefCount = thiefCount,
                        cctvInterval = cctvCycle,
                        missionCount = missionCount,
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

            gameRoomRepository.leaveRoom(roomId)

            _uiEvent.emit(GameRoomUiEvent.NavigateToHome())
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
            }
        }
    }

    // 방장 위임하기
    fun delegateHost(targetMemberId: Long) {
        if (!_isHost.value) return

        viewModelScope.launch {
            _uiState.value = UiState.Loading // 로딩 상태 표시
            roomSocketManager.delegateOwner(targetMemberId)
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