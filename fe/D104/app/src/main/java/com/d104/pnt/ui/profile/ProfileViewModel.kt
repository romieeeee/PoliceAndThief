package com.d104.pnt.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.ProfileResponse
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.ProfileRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val memberId: StateFlow<Long> = authRepository.getMemberId()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    // 화면 상태
    private val _profileState = MutableStateFlow<UiState<ProfileResponse>>(UiState.Idle)
    val profileState: StateFlow<UiState<ProfileResponse>> = _profileState.asStateFlow()

    init {
        observeMemberId()
    }

    private fun observeMemberId() {
        viewModelScope.launch {
            memberId.collectLatest { id ->
                if (id != 0L) {
                    Timber.d("Member ID 감지됨: $id. 프로필 조회를 시작합니다.")
                    fetchMyProfile(id)
                }
            }
        }
    }

    // 내 프로필 조회
    fun fetchMyProfile(id: Long = memberId.value) {
        if (id == 0L) return

        viewModelScope.launch {
            _profileState.value = UiState.Loading

            val result = profileRepository.getMyProfile(id)

            when (result) {
                is BaseResult.Success -> {
                    _profileState.value = UiState.Success(result.data)
                    Timber.d("프로필 조회 성공")
                }
                is BaseResult.Error -> {
                    _profileState.value = UiState.Error(result.error.message)
                    Timber.e("프로필 조회 실패: ${result.error.message}")
                }
            }
        }
    }

    // 프로필 수정
    fun updateProfile(nickname: String, avatarUrl: String) {
        val currentId = memberId.value
        if (currentId == 0L) return

        viewModelScope.launch {
            // 수정 요청
            val result = profileRepository.updateProfile(currentId, nickname, avatarUrl)

            when (result) {
                is BaseResult.Success -> {
                    _profileState.value = UiState.Success(result.data)
                    Timber.d("프로필 수정 성공")
                }
                is BaseResult.Error -> {
                    Timber.e("프로필 수정 실패: ${result.error.message}")
                }
            }
        }
    }
}