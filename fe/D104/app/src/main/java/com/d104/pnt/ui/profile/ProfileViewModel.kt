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
    private val authRepository: AuthRepository // ✅ 1. AuthRepository 주입
) : ViewModel() {

    val memberId: StateFlow<Long> = authRepository.getMemberId()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    // 프로필 UI 상태
    private val _profileState = MutableStateFlow<UiState<ProfileResponse>>(UiState.Idle)
    val profileState: StateFlow<UiState<ProfileResponse>> = _profileState.asStateFlow()

    init {
        observeMemberId()
    }

    /**
     * 내 ID가 로드되는 순간 프로필 조회를 실행하는 로직
     */
    private fun observeMemberId() {
        viewModelScope.launch {
            memberId.collectLatest { id ->
                if (id != 0L) {
                    Timber.d("Member ID detected: $id. Fetching profile...")
                    fetchMyProfile(id)
                }
            }
        }
    }

    /**
     * 프로필 조회
     */
    fun fetchMyProfile(id: Long = memberId.value) {
        if (id == 0L) return

        viewModelScope.launch {
            _profileState.value = UiState.Loading

            when (val result = profileRepository.getMyProfile(id)) {
                is BaseResult.Success -> {
                    _profileState.value = UiState.Success(result.data)
                    Timber.d("Profile fetch successful")
                }
                is BaseResult.Error -> {
                    _profileState.value = UiState.Error(result.error.message)
                    Timber.e("Profile fetch failed: ${result.error.message}")
                }
            }
        }
    }

    /**
     * 프로필 수정
     */
    fun updateProfile(nickname: String, avatarUrl: String) {
        val currentId = memberId.value
        if (currentId == 0L) return

        viewModelScope.launch {
            // (선택) 수정 중에도 로딩을 띄우고 싶다면
            // _profileState.value = UiState.Loading

            val result = profileRepository.updateProfile(currentId, nickname, avatarUrl)

            when (result) {
                is BaseResult.Success -> {
                    _profileState.value = UiState.Success(result.data)
                    Timber.d("Profile update successful")
                }
                is BaseResult.Error -> {
                    // 에러 발생 시 기존 데이터 유지를 위해 Toast만 띄우거나 에러 상태로 전환
                    // 여기서는 에러 메시지를 전달
                    Timber.e("Profile update failed: ${result.error.message}")
                    // _profileState.value = UiState.Error(result.error.message)
                }
            }
        }
    }
}