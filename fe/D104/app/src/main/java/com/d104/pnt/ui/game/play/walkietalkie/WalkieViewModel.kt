package com.d104.pnt.ui.game.play.walkietalkie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.WalkieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 무전기 ViewModel
 */
@HiltViewModel
class WalkieViewModel @Inject constructor(
    private val walkieRepository: WalkieRepository
) : ViewModel() {

    // Repository 상태
    val isConnected: StateFlow<Boolean> = walkieRepository.isConnected
    val isMicEnabled: StateFlow<Boolean> = walkieRepository.isMicEnabled
    val participantCount: StateFlow<Int> = walkieRepository.participantCount

    // UI 상태
    private val _walkieState = MutableStateFlow<WalkieState>(WalkieState.Disconnected)
    val walkieState: StateFlow<WalkieState> = _walkieState.asStateFlow()

    /**
     * 무전기 연결
     */
    fun connect(serverUrl: String, token: String, roomName: String) {
        viewModelScope.launch {
            try {
                _walkieState.value = WalkieState.Connecting
                walkieRepository.connect(serverUrl, token, roomName)
                _walkieState.value = WalkieState.Connected
                Timber.d("Walkie connected")
            } catch (e: Exception) {
                _walkieState.value = WalkieState.Error(e.message ?: "Connection failed")
                Timber.e(e, "Failed to connect walkie")
            }
        }
    }

    /**
     * 무전기 연결 해제
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                walkieRepository.disconnect()
                _walkieState.value = WalkieState.Disconnected
                Timber.d("Walkie disconnected")
            } catch (e: Exception) {
                Timber.e(e, "Error disconnecting walkie")
            }
        }
    }

    /**
     * Push-To-Talk 시작
     */
    fun startTalking() {
        viewModelScope.launch {
            walkieRepository.enableMic()
            Timber.d("Walkie: Mic enabled")
        }
    }

    /**
     * Push-To-Talk 중지
     */
    fun stopTalking() {
        viewModelScope.launch {
            walkieRepository.disableMic()
            Timber.d("Walkie: Mic disabled")
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}

/**
 * 무전기 상태
 */
sealed class WalkieState {
    object Disconnected : WalkieState()
    object Connecting : WalkieState()
    object Connected : WalkieState()
    data class Error(val message: String) : WalkieState()
}