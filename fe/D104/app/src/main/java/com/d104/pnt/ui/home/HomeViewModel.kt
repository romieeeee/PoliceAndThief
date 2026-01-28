package com.d104.pnt.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.AuthRepository
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
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _joinCode = MutableStateFlow("")
    val joinCode: StateFlow<String> = _joinCode.asStateFlow()

    fun updateJoinCode(newCode: String) {
        _joinCode.value = newCode
    }

    /**
     * 로그아웃
     */
    fun testLogout() {
        viewModelScope.launch {

            when (val result = authRepository.logout()) {
                is BaseResult.Success -> {
                    Timber.d("Logout successful")
                }

                is BaseResult.Error -> {
                    Timber.e("Logout failed: ${result.error.message}")
                }
            }
        }
    }

    sealed interface HomeUiEvent {
        object NavigateToIntro : HomeUiEvent
        data class ShowMessage(val message: String) : HomeUiEvent
        data class ShowError(val message: String) : HomeUiEvent
    }

    private val _uiEvent = MutableSharedFlow<HomeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun logout() {
        viewModelScope.launch {
            when (val result = authRepository.logout()) {
                is BaseResult.Success -> {
                    Timber.d("Logout successful")
                    _uiEvent.emit(HomeUiEvent.ShowMessage("로그아웃되었습니다"))
                    _uiEvent.emit(HomeUiEvent.NavigateToIntro)
                }
                is BaseResult.Error -> {
                    Timber.e("Logout failed: ${result.error.message}")
                    _uiEvent.emit(HomeUiEvent.ShowError("로그아웃 실패: ${result.error.message}"))
                }
            }
        }
    }
}

