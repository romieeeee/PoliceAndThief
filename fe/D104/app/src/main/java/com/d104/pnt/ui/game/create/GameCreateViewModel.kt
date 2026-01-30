package com.d104.pnt.ui.game.create

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.data.remote.model.response.CreateGameRoomResponse
import com.d104.pnt.data.repository.GameRoomRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.util.getSingleLocation
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GameCreateViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val gameRoomRepository: GameRoomRepository,
) : ViewModel() {
    val userLocation = locationRepository.currentLocation
    val polygonPoints = locationRepository.polygonPoints
    val prisonLocation = locationRepository.prisonLocation

    private val _gameName = MutableStateFlow("")
    private val _totalPlayers = MutableStateFlow(25)
    private val _gameTime = MutableStateFlow(20)
    private val _missionCount = MutableStateFlow(5)
    private val _cctvCycle = MutableStateFlow(10)
    private val _policeCount = MutableStateFlow(9)
    private val _thiefCount = MutableStateFlow(_totalPlayers.value - _policeCount.value)

    val gameName: StateFlow<String> = _gameName.asStateFlow()
    val totalPlayers: StateFlow<Int> = _totalPlayers.asStateFlow()
    val gameTime: StateFlow<Int> = _gameTime.asStateFlow()
    val missionCount: StateFlow<Int> = _missionCount.asStateFlow()
    val cctvCycle: StateFlow<Int> = _cctvCycle.asStateFlow()
    val policeCount: StateFlow<Int> = _policeCount.asStateFlow()
    val thiefCount: StateFlow<Int> = _thiefCount.asStateFlow()

    fun updateGameName(newName: String) {
        _gameName.value = newName
    }
    fun updateTotalPlayers(plus: Boolean) {
        if (plus && _totalPlayers.value < 30) _totalPlayers.value += 1
        else if (!plus && _totalPlayers.value > 5) _totalPlayers.value -= 1
        _policeCount.value = _totalPlayers.value - _thiefCount.value
    }
    fun updateGameTime(plus: Boolean) {
        if (plus && _gameTime.value < 60) _gameTime.value += 5
        else if (!plus && _gameTime.value > 5) {
            _gameTime.value -= 5
            if (_cctvCycle.value > _gameTime.value) _cctvCycle.value = _gameTime.value
        }
    }
    fun updateMissionCount(plus: Boolean) {
        if (plus && _missionCount.value < _thiefCount.value) _missionCount.value += 1
        else if (!plus && _missionCount.value > 0) _missionCount.value -= 1
    }
    fun updateCctvCycle(plus: Boolean) {
        if (plus && _cctvCycle.value < _gameTime.value) _cctvCycle.value += 1
        else if (!plus && _cctvCycle.value > 0) _cctvCycle.value -= 1
    }
    fun updatePoliceCount(newCount: Int) {
        _policeCount.value = newCount
        _thiefCount.value = _totalPlayers.value - _policeCount.value
        if (_thiefCount.value < _missionCount.value) _missionCount.value = _thiefCount.value
    }

    fun dismissCreateGame() {
        locationRepository.dismissCreateGame()
    }

    // 화면 진입 시 호출할 함수
    fun setDefaultSettings(context: Context) {
        viewModelScope.launch {
            // 1. 1회성 위치 가져오기 (만들어둔 확장 함수 사용)
            val location = context.getSingleLocation()
            if (location != null) {
                // 2. [핵심] 가져온 위치를 Repository에 저장!
                locationRepository.updateCurrentLocation(location)
                locationRepository.createDefaultPolygon(location)
                locationRepository.setPrisonLocation(
                    LatLng(
                        userLocation.value!!.latitude,
                        userLocation.value!!.longitude
                    )
                )
            } else {
                Timber.e("위치를 가져오지 못했습니다.")
            }
        }
    }

    private val _gameRoomState = MutableStateFlow<UiState<CreateGameRoomResponse>>(UiState.Idle)
    val gameRoomState: StateFlow<UiState<CreateGameRoomResponse>> = _gameRoomState.asStateFlow()

    fun createGameRoom(
        playerCount: Int,
        timeLimit: Int,
        policeCount: Int,
        thiefCount: Int,
        prison: Location,
        polygon: List<Location>
    ) {
        viewModelScope.launch {
            _gameRoomState.value = UiState.Loading
            when (val result = gameRoomRepository.createGameRoom(
                playerCount,
                timeLimit,
                policeCount,
                thiefCount,
                prison,
                polygon
            )) {
                is BaseResult.Success -> {
                    _gameRoomState.value = UiState.Success(result.data)
                    Timber.d("GameRoomCreate: ${result.data}")
                }
                is BaseResult.Error -> {
                    _gameRoomState.value = UiState.Error(result.error.message)
                    Timber.d("GameRoomCreate Error: ${result.error.message}")
                }
            }
        }
    }

    fun isValid (
        playerCount: Int,
        timeLimit: Int,
        policeCount: Int,
        thiefCount: Int,
        polygon: List<Location>
    ): Boolean {
        if (playerCount < 5 || playerCount > 30) return false
        if (timeLimit < 5 || timeLimit > 60) return false
        if (policeCount < 1 || policeCount >= playerCount) return false
        if (thiefCount < 1 || thiefCount >= playerCount) return false
        if (polygon.size < 3) return false
        return true
    }
}