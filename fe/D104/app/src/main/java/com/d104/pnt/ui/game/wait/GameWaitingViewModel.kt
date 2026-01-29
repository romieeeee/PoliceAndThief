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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GameWaitingViewModel @Inject constructor(
    private val gameRoomRepository: GameRoomRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val roomId: Long = savedStateHandle.get<Long>(NavArgs.ROOM_ID) ?: 0L

    // 내 Member ID
    val myMemberId: StateFlow<Long> = authRepository.getMemberId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

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

    // 역할 변경 중 상태
    private val _changingRoleMemberIds = MutableStateFlow<Set<Long>>(emptySet())

    private var pollingJob: Job? = null

    init {
        loadRoomSettings()
        startPolling()
    }

    // 방 설정 조회
    private fun loadRoomSettings() {
        viewModelScope.launch {
            when (val result = gameRoomRepository.getRoomSettings(roomId)) {
                is BaseResult.Success -> {
                    val data = result.data
                    _roomInfo.value = GameRoomInfoState(
                        roomCode = "ROOM-${data.roomId}",
                        maxCount = data.playerCount,
                        policeCount = data.policeCount,
                        thiefCount = data.thiefCount,
                        timeLimit = data.timeLimit
                    )
                }
                is BaseResult.Error -> {
                    Timber.e("방 설정 로드 실패: ${result.error.message}")
                    _uiState.value = UiState.Error(result.error.message)
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

                val uiPlayers = items.map { item ->
                    // 내 상태 갱신
                    if (item.memberId == currentMemberId) {
                        _isHost.value = item.host
                        _isMeReady.value = item.ready
                    }

                    WaitingPlayer(
                        id = item.memberId,
                        nickname = item.nickname,
                        role = if (item.role == "POLICE") GameRole.POLICE else GameRole.THIEF,
                        isReady = item.ready,
                        profileUrl = item.profileImageUrl,
                        // 로컬 상태와 결합
                        isChangingRole = _changingRoleMemberIds.value.contains(item.memberId)
                    )
                }
                _players.value = uiPlayers
            }
            is BaseResult.Error -> {
                Timber.e("멤버 조회 실패: ${result.error.message}")
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

    // 방 나가기
    fun leaveRoom() {
        viewModelScope.launch {
            gameRoomRepository.leaveRoom(roomId)
            pollingJob?.cancel()
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

data class GameRoomInfoState(
    val roomCode: String = "",
    val maxCount: Int = 0,
    val policeCount: Int = 0,
    val thiefCount: Int = 0,
    val timeLimit: Int = 0
)