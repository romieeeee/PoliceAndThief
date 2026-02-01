package com.d104.pnt.ui.game.play

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.getSingleLocation
import com.d104.pnt.util.socket.GameSocketManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GamePlayViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val authRepository: AuthRepository,
    private val gameSocketManager: GameSocketManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val gameId: Long = savedStateHandle.get<Long>(NavArgs.GAME_ID) ?: 0L

    // UI 이벤트
    private val _uiEvent = MutableSharedFlow<GamePlayUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val userLocation = locationRepository.currentLocation
    val polygonPoints = locationRepository.polygonPoints
    val prisonLocation = locationRepository.prisonLocation

    private val _allMembers = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())

    private val _thiefMembers = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())
    val thiefMembers: StateFlow<List<GameMemberSocketDto>> = _thiefMembers.asStateFlow()

    init {
        setupSocketListeners()
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

    private fun setupSocketListeners() {
        // 전체 게임 정보 동기화
        gameSocketManager.setOnGameInfoSynced { data ->
            viewModelScope.launch {
                try {
                    val membersArray = data.optJSONArray("members")
                    if (membersArray != null) {
                        val newMembers = mutableListOf<GameMemberSocketDto>()
                        for (i in 0 until membersArray.length()) {
                            val memberJson = membersArray.getJSONObject(i)
                            newMembers.add(GameMemberSocketDto.fromJson(memberJson))
                        }

                        updateMembersList(newMembers)
                        Timber.d("GamePlayViewModel: 전체 멤버 동기화 완료 (${newMembers.size}명)")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "GamePlayViewModel: 게임 정보 파싱 실패")
                }
            }
        }

        // 실시간 상태 변경
        gameSocketManager.setOnMemberStatusChanged { gameId, thiefId, status, arrestedAt ->
            viewModelScope.launch {
                val currentList = _allMembers.value.toMutableList()
                val targetIndex = currentList.indexOfFirst { it.memberId == thiefId }

                if (targetIndex != -1) {
                    val oldData = currentList[targetIndex]
                    val newData = oldData.copy(rawStatus = status)
                    currentList[targetIndex] = newData

                    updateMembersList(currentList)
                    Timber.d("GamePlayViewModel: 도둑($thiefId) 상태 변경 -> $status")
                }
            }
        }

        gameSocketManager.setOnGameEnded { winnerPosition, message ->
            Timber.d("🏁 게임 종료 수신: $winnerPosition 승리")

            // 2. 상세 결과 요청 (post after game end)
            gameSocketManager.postAfterGameEnd(gameId)
        }

        // 3. 게임 상세 결과 수신
        gameSocketManager.setOnEndGameAfter { data ->
            viewModelScope.launch {
                // 상세 결과를 저장하거나 처리 (MVP 정보 등)
                // data.optJSONObject("mvp") ...

                // 4. 뉴스 화면으로 이동 이벤트 발생
                _uiEvent.emit(GamePlayUiEvent.NavigateToNews(gameId))
            }
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

        gameSocketManager.leaveGame()
        gameSocketManager.removeAllListeners()
    }

}

sealed class GamePlayUiEvent {
    data class NavigateToNews(val gameId: Long) : GamePlayUiEvent()
}