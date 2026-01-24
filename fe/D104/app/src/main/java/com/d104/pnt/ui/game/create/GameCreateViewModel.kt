package com.d104.pnt.ui.game.create

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.utils.helper.getSingleLocation
import kotlinx.coroutines.launch


class GameCreateViewModel : ViewModel() {

    // 화면 진입 시 호출할 함수
    fun getCurrentLocation(context: Context) {
        viewModelScope.launch {
            // 1. 1회성 위치 가져오기 (만들어둔 확장 함수 사용)
            val location = context.getSingleLocation()

            if (location != null) {
                // 2. [핵심] 가져온 위치를 Repository에 저장!
                LocationRepository.updateCurrentLocation(location)
                Log.d("GameViewModel", "위치 갱신 및 저장 완료: ${location.latitude}")
            } else {
                Log.e("GameViewModel", "위치를 가져오지 못했습니다.")
            }
        }
    }
}