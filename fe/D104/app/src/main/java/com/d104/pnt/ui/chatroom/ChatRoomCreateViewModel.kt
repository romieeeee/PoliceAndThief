package com.d104.pnt.ui.chatroom

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.domain.model.GeoLocationInfo
import com.d104.pnt.util.getSingleLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatRoomCreateViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _currentAddress = MutableStateFlow(GeoLocationInfo("위치", "찾는", "중..."))
    val currentAddress: StateFlow<GeoLocationInfo> = _currentAddress.asStateFlow()


    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun updateDescription(newDescription: String) {
        _description.value = newDescription
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
        }
    }
}