package com.d104.pnt.ui.game.wait

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.data.remote.model.response.GameMemberListResponse
import com.d104.pnt.data.remote.model.response.GameRoomSettingsResponse
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRoomRepository
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.domain.model.RoomInfoResponse
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.socket.RoomSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GameWaitingViewModel @Inject constructor(
    private val gameRoomRepository: GameRoomRepository,
    private val authRepository: AuthRepository,
    private val roomSocketManager: RoomSocketManager,
    savedStateHandle: SavedStateHandle,

    ) : ViewModel() {
    private val roomId: Long = savedStateHandle.get<Long>(NavArgs.ROOM_ID) ?: 0L

    // UI State
    private val _myMemberId = MutableStateFlow(0L)
    val myMemberId: StateFlow<Long> = _myMemberId.asStateFlow()

    private val _players = MutableStateFlow<List<WaitingPlayer>>(emptyList())
    val players: StateFlow<List<WaitingPlayer>> = _players.asStateFlow()

    private val _roomInfo = MutableStateFlow(GameRoomInfoState())
    val roomInfo: StateFlow<GameRoomInfoState> = _roomInfo.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    private val _isMeReady = MutableStateFlow(false)
    val isMeReady: StateFlow<Boolean> = _isMeReady.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<GameWaitingUiEvent>()
    val uiEvent: SharedFlow<GameWaitingUiEvent> = _uiEvent.asSharedFlow()

    init {
        Timber.d("GameWaitingViewModel 초기화 - roomId: $roomId")

        // 1. 콜백 먼저 설정
        setupRoomCallbacks()

        viewModelScope.launch {
            authRepository.getMemberId().collect { id ->
                if (id != 0L) {
                    _myMemberId.value = id
                    Timber.d("내 멤버 ID: $id")

                    connectToRoom()
                }
            }
        }
    }

    /**
     * Socket 연결 및 방 입장
     */
    private fun connectToRoom() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            authRepository.getAccessToken().collect { token ->
                if (token.isNotEmpty()) {
                    Timber.d("🔌 Room 소켓 연결 시작")

                    // Socket 연결
                    roomSocketManager.connect(token)

                    // 연결 대기
                    var retryCount = 0
                    while (!roomSocketManager.isConnected() && retryCount < 30) {
                        delay(100)
                        retryCount++
                    }

                    if (roomSocketManager.isConnected()) {
                        Timber.d("✅ Room 소켓 연결 성공")

                        // 방 입장 (자동으로 room info 요청됨)
                        roomSocketManager.joinRoom(roomId) { success, message ->
                            if (success) {
                                Timber.d("✅ 방 입장 성공 - UI는 Socket 콜백에서 업데이트됨")
                            } else {
                                Timber.e("❌ 방 입장 실패: $message")
                                viewModelScope.launch {
                                    _uiState.value = UiState.Error("방 입장 실패")
                                    _uiEvent.emit(GameWaitingUiEvent.NavigateToHome("방 입장 실패"))
                                }
                            }
                        }
                    } else {
                        Timber.e("❌ Room 소켓 연결 타임아웃")
                        _uiState.value = UiState.Error("서버 연결 실패")
                        _uiEvent.emit(GameWaitingUiEvent.NavigateToHome("서버 연결 실패"))
                    }
                }
                return@collect
            }
        }
    }

    /**
     * Socket 콜백 설정
     */
    private fun setupRoomCallbacks() {
        // ✅ 전체 방 정보 수신 (UI 업데이트의 유일한 진실의 원천)
        roomSocketManager.setOnFullRoomInfoReceived { data ->
            Timber.d("📥 [PRIMARY] 전체 방 정보 수신 - UI 업데이트")
            parseFullRoomInfo(data)
            _uiState.value = UiState.Idle  // 로딩 해제
        }

        // ✅ 방 설정 업데이트
        roomSocketManager.setOnRoomInfoUpdated { data ->
            Timber.d("📥 방 설정 업데이트")
            parseRoomSettings(data)
        }

        // ✅ Ready 상태 업데이트
        roomSocketManager.setOnReadyUpdated { roomId, memberId, isReady ->
            Timber.d("📥 Ready 업데이트: memberId=$memberId, ready=$isReady")
            updatePlayerReady(memberId, isReady)
        }

        // ✅ 포지션 업데이트
        roomSocketManager.setOnPositionUpdated { roomId, memberId, position ->
            Timber.d("📥 포지션 업데이트: memberId=$memberId, position=$position")
            updatePlayerPosition(memberId, position)
        }

        // ✅ 멤버 강퇴
        roomSocketManager.setOnMemberKicked { memberId ->
            Timber.d("📥 멤버 강퇴: $memberId")
            if (memberId == _myMemberId.value) {
                viewModelScope.launch {
                    _uiEvent.emit(GameWaitingUiEvent.NavigateToHome("방에서 강퇴되었습니다."))
                }
            } else {
                removePlayer(memberId)
            }
        }

        // ✅ 멤버 퇴장
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
            // 방장 여부 확인
            _isHost.value = data.room.hostMemberId == _myMemberId.value

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
                if (member.memberId == _myMemberId.value) {
                    _isMeReady.value = member.ready
                }

                WaitingPlayer(
                    id = member.memberId,
                    nickname = member.memberDetail.profile.nickname ?: "이름 없음",
                    role = if (member.preferPosition == "POLICE") GameRole.POLICE else GameRole.THIEF,
                    isReady = member.ready,
                    profileUrl = member.memberDetail.profile.avatarUrl
                )
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
        _players.value = _players.value.map { player ->
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
                        else -> GameRole.THIEF
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
        _players.value = _players.value.filter { it.id != memberId }
    }

// ==================== User Actions ====================

    /**
     * 준비 상태 변경 (HTTP → Socket 업데이트)
     */
    fun toggleReady() {
        viewModelScope.launch {
            val nextState = !_isMeReady.value

            // HTTP 요청
            when (gameRoomRepository.toggleReady(roomId, nextState)) {
                is BaseResult.Success -> {
                    Timber.d("✅ Ready 요청 성공 - Socket 업데이트 대기")
                    // UI는 setOnReadyUpdated에서 업데이트됨
                }
                is BaseResult.Error -> {
                    Timber.e("❌ Ready 요청 실패")
                    _uiState.value = UiState.Error("준비 상태 변경 실패")
                }
            }
        }
    }

    /**
     * 역할 변경 (HTTP → Socket 업데이트)
     */
    fun changeRole() {
        viewModelScope.launch {
            val myPlayer = _players.value.find { it.id == _myMemberId.value } ?: return@launch
            val nextRole = if (myPlayer.role == GameRole.POLICE) "THIEF" else "POLICE"

            // HTTP 요청
            when (gameRoomRepository.changePosition(roomId, nextRole)) {
                is BaseResult.Success -> {
                    Timber.d("✅ 포지션 변경 요청 성공 - Socket 업데이트 대기")
                    // UI는 setOnPositionUpdated에서 업데이트됨
                }
                is BaseResult.Error -> {
                    Timber.e("❌ 포지션 변경 실패")
                    _uiState.value = UiState.Error("역할 변경 실패")
                }
            }
        }
    }

    /**
     * 방 설정 변경 (HTTP → Socket 업데이트)
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

            when (gameRoomRepository.updateRoomSettings(
                roomId = roomId,
                playerCount = safeMaxCount,
                timeLimit = timeLimit,
                cctvInterval = cctvCycle,
                policeCount = safePoliceCount,
                thiefCount = thiefCount,
                missionCount = missionCount,
                prison = current.prison,
                polygon = current.polygon
            )) {
                is BaseResult.Success -> {
                    Timber.d("✅ 방 설정 변경 성공 - Socket 업데이트 대기")
                    // UI는 setOnRoomInfoUpdated에서 업데이트됨
                }
                is BaseResult.Error -> {
                    Timber.e("❌ 방 설정 변경 실패")
                    _uiState.value = UiState.Error("설정 변경 실패")
                }
            }
        }
    }

    /**
     * 강퇴 (HTTP → Socket 업데이트)
     */
    fun kickPlayer(targetMemberId: Long, reason: String) {
        if (!_isHost.value) return

        viewModelScope.launch {
            when (gameRoomRepository.kickPlayer(roomId, targetMemberId, reason)) {
                is BaseResult.Success -> {
                    Timber.d("✅ 강퇴 성공 - Socket 업데이트 대기")
                    // UI는 setOnMemberKicked에서 업데이트됨
                }
                is BaseResult.Error -> {
                    _uiState.value = UiState.Error("강퇴 실패")
                }
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
     * 방 나가기
     */
    fun leaveRoom() {
        viewModelScope.launch {
            // HTTP 요청
            gameRoomRepository.leaveRoom(roomId)

            // Socket 연결 해제
            roomSocketManager.leaveRoom()

            // 홈으로 이동
            _uiEvent.emit(GameWaitingUiEvent.NavigateToHome())
        }
    }

    override fun onCleared() {
        super.onCleared()
        roomSocketManager.disconnect()
        Timber.d("GameWaitingViewModel cleared")
    }
}