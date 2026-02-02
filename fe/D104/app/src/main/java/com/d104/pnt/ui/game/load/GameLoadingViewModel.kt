package com.d104.pnt.ui.game.load

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.navigation.NavArgs
import com.d104.pnt.util.socket.GameSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GameLoadingViewModel @Inject constructor(
    private val gameSocketManager: GameSocketManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_ROLE = "role"
        private const val TOTAL_SECONDS = 60
    }

    // Navigation argument에서 role 가져오기
    val role: GameRole = savedStateHandle.get<String>(KEY_ROLE)?.let {
        GameRole.fromName(it)
    } ?: GameRole.THIEF // 기본값

    private val _remainingTime = MutableStateFlow(TOTAL_SECONDS)
    val remainingTime: StateFlow<Int> = _remainingTime

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    private val _message = MutableStateFlow(getInitialMessage())
    val message: StateFlow<String> = _message

    private var gameStartTime: Long? = null

    init {
        Timber.d("GameLoadingViewModel initialized with role: $role")

        setupGameStartListener()

        startCountdown()
    }

    private fun setupGameStartListener() {
        gameSocketManager.setOnGameStarted { gameId, startTime ->
            Timber.d("🏁 [Loading] 게임 시작 신호 수신: $startTime")
            gameStartTime = System.currentTimeMillis()
            _remainingTime.value = TOTAL_SECONDS
        }

        // 서버로부터 싱크를 받았을 때 현재 남은 시간을 계산
        gameSocketManager.setOnGameInfoSynced { data ->
            val startTime = data.optLong("startTime") // 서버에서 게임이 실제 시작된 Timestamp
            val currentTime = System.currentTimeMillis()

            // (시작시간 + 60초) - 현재시간 = 내가 화면에서 보여줘야 할 남은 시간
            val elapsedSeconds = (currentTime - startTime) / 1000
            val remaining = (TOTAL_SECONDS - elapsedSeconds).toInt()

            if (remaining > 0) {
                _remainingTime.value = remaining
                Timber.d("⏰ 서버와 시간 동기화: 남은 시간 ${remaining}초")
            } else {
                // 이미 1분이 지났다면 즉시 인게임 진입
                _isFinished.value = true
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (_remainingTime.value > 0) {
                delay(1000L)
                _remainingTime.value--
                updateMessage()
            }
            _isFinished.value = true
            Timber.d("✅ 도둑 도망 시간 종료 - 인게임 진입")
        }
    }

    private fun getInitialMessage(): String {
        return when (role) {
            GameRole.POLICE -> "경찰 팀원들과 무전기로 소통하며\n도둑들을 체포하세요!"
            GameRole.THIEF -> "경찰을 피해 미션을 완수하고\n생존하세요!"
            else -> "곧 게임이 시작됩니다!\n역할을 확인하세요."
        }
    }

    private fun updateMessage() {
        _message.value = when {
            _remainingTime.value > 45 -> getInitialMessage()
            _remainingTime.value > 30 -> when (role) {
                GameRole.POLICE -> "도둑들의 위치를 파악하세요"
                GameRole.THIEF -> "은신 장소를 찾으세요"
                else -> "주변을 탐색하세요"
            }

            _remainingTime.value > 10 -> when (role) {
                GameRole.POLICE -> "팀원들과 협력하세요"
                GameRole.THIEF -> "경찰의 포위망을 조심하세요"
                else -> "준비하세요"
            }

            else -> "곧 게임이 시작됩니다!"
        }
    }
}