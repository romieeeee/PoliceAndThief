package com.d104.pnt.ui.game.play.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.PresignedUrlResponse
import com.d104.pnt.data.repository.GameSessionRepository
import com.d104.pnt.data.repository.ImageRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.util.socket.GameSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
    private val gameSessionRepository: GameSessionRepository,
    private val gameSocketManager: GameSocketManager
) : ViewModel() {
    private val _missionState = MutableStateFlow<UiState<PresignedUrlResponse>>(UiState.Idle)

    fun uploadImage(image: File, missionId: Long) {
        viewModelScope.launch {
            _missionState.value = UiState.Loading
            when (val result = imageRepository.getPresignedUrlToMission(image.name)) {
                is BaseResult.Success -> {
                    imageRepository.uploadImage(result.data.presignedUrl, image)
                    gameSocketManager.submitMissionImage(
                        gameSessionRepository.myMemberId.value,
                        missionId,
                        result.data.presignedUrl
                    )
                }
                is BaseResult.Error -> {
                    _missionState.value = UiState.Error(result.error.message)
                }
            }
        }
    }
}