package com.d104.pnt.ui.game.play.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.MissionPresignedUrlResponse
import com.d104.pnt.data.remote.model.response.PresignedUrlResponse
import com.d104.pnt.data.repository.AuthRepository
import com.d104.pnt.data.repository.GameSessionRepository
import com.d104.pnt.data.repository.ImageRepository
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.util.socket.GameSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val gameSessionRepository: GameSessionRepository,
) : ViewModel() {
    fun uploadImage(image: File, missionId: Long) {
        gameSessionRepository.uploadMissionImage(image, missionId)
    }
}