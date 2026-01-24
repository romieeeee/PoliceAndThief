package com.d104.pnt.ui.game.load

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.d104.pnt.domain.model.GameRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 게임 로딩 화면 ViewModel
 * 역할별 카운트다운 및 게임 시작 준비
 */
class GameLoadingViewModel(
    private val totalSeconds: Int = 5,
    val role: GameRole
) : ViewModel() {

    private val _remainingTime = MutableStateFlow(totalSeconds)
    val remainingTime: StateFlow<Int> = _remainingTime

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    // 역할별 메시지
    private val _message = MutableStateFlow(getInitialMessage())
    val message: StateFlow<String> = _message

    init {
        startCountdown()
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (_remainingTime.value > 0) {
                delay(1000L)
                _remainingTime.value--
                updateMessage()
            }
            _isFinished.value = true
        }
    }

    private fun getInitialMessage(): String {
        return when (role) {
            GameRole.POLICE -> "경찰 팀원들과 무전기로 소통하며\n도둑들을 체포하세요!"
            GameRole.THIEF -> "경찰을 피해 미션을 완수하고\n생존하세요!"
        }
    }

    private fun updateMessage() {
        _message.value = when {
            _remainingTime.value > 45 -> getInitialMessage()
            _remainingTime.value > 30 -> when (role) {
                GameRole.POLICE -> "도둑들의 위치를 파악하세요"
                GameRole.THIEF -> "은신 장소를 찾으세요"
            }

            _remainingTime.value > 10 -> when (role) {
                GameRole.POLICE -> "팀원들과 협력하세요"
                GameRole.THIEF -> "경찰의 포위망을 조심하세요"
            }

            else -> "곧 게임이 시작됩니다!"
        }
    }


}

/**
 * ViewModel Factory
 */
class GameLoadingViewModelFactory(
    private val role: GameRole
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameLoadingViewModel::class.java)) {
            return GameLoadingViewModel(role = role) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}