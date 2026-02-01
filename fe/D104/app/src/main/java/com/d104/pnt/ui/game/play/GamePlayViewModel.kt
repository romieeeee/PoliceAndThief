package com.d104.pnt.ui.game.play

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.GameMemberSocketDto
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRepository
import com.d104.pnt.data.repository.GameSessionRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.service.game.GameActiveService
import com.d104.pnt.util.StepSensorManager
import com.d104.pnt.util.socket.GameSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GamePlayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val authRepository: AuthRepository,
    private val gameRepository: GameRepository,
    private val gameSessionRepository: GameSessionRepository,
    private val gameSocketManager: GameSocketManager,
    private val stepSensorManager: StepSensorManager
) : ViewModel() {
    val polygonPoints = locationRepository.polygonPoints
    val prisonLocation = locationRepository.prisonLocation
    val userLocation = locationRepository.currentLocation

    val members = gameSessionRepository.members

    val gameId = gameSessionRepository.gameId

    val gameStatus = gameSessionRepository.gameStatus

    val missions = gameSessionRepository.missions

    private val _thiefMembers = MutableStateFlow<List<GameMemberSocketDto>>(emptyList())
    val thiefMembers: StateFlow<List<GameMemberSocketDto>> = _thiefMembers.asStateFlow()

    init{
        viewModelScope.launch {
            val token = authRepository.getAccessToken().first()
            if (token.isNotEmpty() && !gameSocketManager.isConnected()) {
                gameSocketManager.connect(token)

                while (!gameSocketManager.isConnected()) {
                    delay(100)
                }
            }
            gameSessionRepository.gameInit()
            startService(GameActiveService.ACTION_START)
        }
    }

    override fun onCleared() {
        super.onCleared()
        startService(GameActiveService.ACTION_STOP)
        gameSessionRepository.leaveGame()
    }

    private fun startService(action: String) {
        Intent(context, GameActiveService::class.java).also {
            it.action = action
            context.startService(it)
        }
    }
}