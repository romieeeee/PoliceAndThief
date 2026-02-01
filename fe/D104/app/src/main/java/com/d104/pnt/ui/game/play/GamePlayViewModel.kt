package com.d104.pnt.ui.game.play

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.util.getSingleLocation
import com.d104.pnt.util.socket.GameSocketManager
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GamePlayViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val authRepository: AuthRepository,
    private val gameSocketManager: GameSocketManager
) : ViewModel() {
    val userLocation = locationRepository.currentLocation
    val polygonPoints = locationRepository.polygonPoints
    val prisonLocation = locationRepository.prisonLocation

    private val _allMembers = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())

    private val _thiefMembers = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())
    val thiefMembers: StateFlow<List<GameMemberSocketDto>> = _thiefMembers.asStateFlow()

    init {
        setupSocketListeners()
    }

    fun initGame(gameId: Long) {
        viewModelScope.launch {
            val token = authRepository.getAccessToken().first()
            if (token.isNotEmpty() && !gameSocketManager.isConnected()) {
                gameSocketManager.connect(token)

                while (!gameSocketManager.isConnected()) {
                    delay(100)
                }
            }

            gameSocketManager.joinGame(gameId)
            Timber.d("GamePlayViewModel: 게임($gameId) 입장 요청 보냄")

            delay(500)
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