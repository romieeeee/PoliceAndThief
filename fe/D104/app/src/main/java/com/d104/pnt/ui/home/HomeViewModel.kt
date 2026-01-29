package com.d104.pnt.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameRoomRepository
import com.d104.pnt.domain.model.common.BaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRoomRepository: GameRoomRepository
) : ViewModel() {

    private val _joinCode = MutableStateFlow("")
    val joinCode: StateFlow<String> = _joinCode.asStateFlow()

    fun updateJoinCode(newCode: String) {
        _joinCode.value = newCode
    }

    sealed interface HomeUiEvent {
        object NavigateToIntro : HomeUiEvent
        data class ShowMessage(val message: String) : HomeUiEvent
        data class ShowError(val message: String) : HomeUiEvent
        data class NavigateToGameRoom(val roomId: Long) : HomeUiEvent
    }

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun joinGame() {
        viewModelScope.launch {
            val code = _joinCode.value
            if (code.isBlank()) {
                _uiEvent.emit(HomeUiEvent.ShowError("참여 코드를 입력해주세요."))
                return@launch
            }

            when (val result = gameRoomRepository.joinGameRoom(code)) {
                is BaseResult.Success -> {
                    val roomId = result.data.roomId
                    Timber.d("Join Game Success: roomId=$roomId")

                    _uiEvent.emit(HomeUiEvent.NavigateToGameRoom(roomId))

                    _joinCode.value = ""
                }
                is BaseResult.Error -> {
                    Timber.e("Join Game Failed: ${result.error.message}")
                    _uiEvent.emit(HomeUiEvent.ShowError(result.error.message))
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.clearAuthData()

            Timber.d("Logout completed")
            _uiEvent.emit(HomeUiEvent.ShowMessage("로그아웃되었습니다"))
            _uiEvent.emit(HomeUiEvent.NavigateToIntro)
        }
    }
}

