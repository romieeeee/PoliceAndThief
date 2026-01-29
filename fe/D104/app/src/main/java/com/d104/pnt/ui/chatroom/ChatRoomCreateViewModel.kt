package com.d104.pnt.ui.chatroom

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.ChatCreateResponse
import com.d104.pnt.data.repository.ChatRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.domain.model.GeoLocationInfo
import com.d104.pnt.domain.model.common.UiState
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.util.getSingleLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatRoomCreateViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _maxMember = MutableStateFlow(100)
    val maxMember: StateFlow<Int> = _maxMember.asStateFlow()

    private val _currentAddress = MutableStateFlow(GeoLocationInfo("현재 위치", "찾는중..."))
    val currentAddress: StateFlow<GeoLocationInfo> = _currentAddress.asStateFlow()

    private val _createChatRoomStats = MutableStateFlow<UiState<ChatCreateResponse>>(UiState.Idle)
    val createChatRoomStats: StateFlow<UiState<ChatCreateResponse>> = _createChatRoomStats.asStateFlow()

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun updateDescription(newDescription: String) {
        _description.value = newDescription
    }

    fun updateMaxMember(plus: Boolean) {
        if (plus && maxMember.value < 200) {
            _maxMember.value += 1
        } else if (!plus && maxMember.value > 50){
            _maxMember.value -= 1
        }
    }

    // 위치 정보를 가져오면서 주소도 같이 업데이트
    fun getLocationInfo(context: Context) {
        viewModelScope.launch {
            val location = context.getSingleLocation()
            if (location != null) {
                // 좌표 -> 주소 변환 호출
                val address = locationRepository.getAddressFromLatLng(
                    location.latitude,
                    location.longitude
                )
                _currentAddress.value = address
            }
            if (_currentAddress.value.major != "세종특별자치시") {
                _currentAddress.value = GeoLocationInfo(
                    major = _currentAddress.value.major,
                    middle = _currentAddress.value.middle,
                    code = _currentAddress.value.code / 10000 * 10000
                )
            }
        }
    }

    fun isValid(): Boolean {
        if (title.value.isEmpty()) {
            return false
        }
        if (description.value.isEmpty()) {
            return false
        }
        return true
    }

    fun createChatRoom() {
        viewModelScope.launch {
            _createChatRoomStats.value = UiState.Loading
            when (val result = chatRepository.createChatRoom(title.value, _currentAddress.value.code, description.value, maxMember.value)) {
                is BaseResult.Success -> {
                    _createChatRoomStats.value = UiState.Success(result.data)
                    Timber.d("${result.data}")
                }
                is BaseResult.Error -> {
                    _createChatRoomStats.value = UiState.Error(result.error.message)
                    Timber.d(result.error.message)
                }
            }
        }
    }
}