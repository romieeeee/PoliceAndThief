package com.d104.pnt.ui.game.wait

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRoomRepository
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.domain.model.RoomInfoResponse
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.socket.RoomSocketManager
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
    }

    private fun syncData() {
        viewModelScope.launch {
            while (!roomSocketManager.isConnected()) { delay(200) }

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

    /**
     * 전체 방 정보 파싱
     */
    private fun parseFullRoomInfo(data: RoomInfoResponse) {
        try {
            val hostId = data.room.hostMemberId
            val myId = _myMemberId.value

            _isHost.value = (hostId == myId && myId != 0L)

            Timber.d("🏠 방장 확인: host=$hostId, me=$myId, 결과=${_isHost.value}")

            // 방 설정 업데이트
            _roomInfo.value = GameRoomInfoState(
                roomCode = data.room.roomCode,
                maxCount = data.roomSetting.playerCount,
                policeCount = data.roomSetting.policeCount,
                thiefCount = data.roomSetting.thiefCount,
                timeLimit = data.roomSetting.timeLimit / 60,
//                missionCount = data.roomSetting.missionCount,
//                cctvCycle = data.roomSetting.,
                prison = Location(data.roomSetting.prisonLat, data.roomSetting.prisonLng)
            )

            // 플레이어 리스트 업데이트
            _players.value = data.members.map { member ->

                if (member.memberId == myId) {
                    _isMeReady.value = member.ready
                }

                WaitingPlayer(
                    id = member.memberId,
                    nickname = member.memberDetail.profile.nickname,
                    role = when (member.preferPosition) {
                        "POLICE" -> GameRole.POLICE
                        "THIEF" -> GameRole.THIEF
                        else -> GameRole.ANY
                    },
                    isReady = member.ready,
                    isHost = member.memberId == hostId,
                    profileUrl = member.memberDetail.profile.avatarUrl
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
                        "UNDECIDED" -> GameRole.UNDECIDED
                        else -> GameRole.ANY
                    }
                )
            } else {
                player
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
    // ==================== User Actions ====================

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
     * 방 설정 변경 (HTTP → Socket)
     */
    fun updateRoomSettings(
        maxCount: Int,
        timeLimit: Int,
        missionCount: Int,
        cctvCycle: Int,
        policeCount: Int
    ) {
        if (!_isHost.value) return

        viewModelScope.launch {
            val current = _roomInfo.value
            val safeMaxCount = maxCount.coerceAtLeast(5)
            val safePoliceCount = policeCount.coerceIn(1, safeMaxCount - 1)
            val thiefCount = safeMaxCount - safePoliceCount

            // 1. [HTTP] 서버 DB 업데이트
            val result = gameRoomRepository.updateRoomSettings(
                roomId = roomId,
                playerCount = safeMaxCount,
                timeLimit = timeLimit,
                cctvInterval = cctvCycle,
                policeCount = safePoliceCount,
                thiefCount = thiefCount,
                missionCount = missionCount,
                prison = current.prison,
                polygon = current.polygon
            )

            when (result) {
                is BaseResult.Success -> {
                    Timber.d("✅ [HTTP] 방 설정 변경 성공")

                    // 2. [Socket] 다른 유저들에게 변경 알림 (추가된 부분)
                    roomSocketManager.updateRoomInfo(
                        playerCount = safeMaxCount,
                        timeLimit = timeLimit,
                        policeCount = safePoliceCount,
                        thiefCount = thiefCount,
                        cctvInterval = cctvCycle,
                        prison = current.prison,
                        polygon = current.polygon
                    )
                }
                is BaseResult.Error -> {
                    _uiState.emit(UiState.Error("설정 변경 실패"))
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

}