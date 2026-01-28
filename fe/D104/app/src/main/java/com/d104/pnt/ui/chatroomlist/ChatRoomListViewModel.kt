package com.d104.pnt.ui.chatroomlist

import androidx.lifecycle.ViewModel
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.data.source.local.RegionCodeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatRoomListViewModel @Inject constructor(
    private val regionManager: RegionCodeManager,
    private val locationRepository: LocationRepository
): ViewModel() {
    // 1. 시/도 목록 (변하지 않음)
    val majorList = regionManager.majorRegions

    // 2. 선택된 상태
    private val _selectedMajor = MutableStateFlow("")
    val selectedMajor = _selectedMajor.asStateFlow()

    private val _selectedMiddle = MutableStateFlow("")
    val selectedMiddle = _selectedMiddle.asStateFlow()

    // 3. 현재 선택된 시/도에 따른 시/군/구 목록
    private val _middleList = MutableStateFlow<List<String>>(emptyList())
    val middleList = _middleList.asStateFlow()

    // 시/도 선택 시 호출
    fun selectMajor(major: String) {
        _selectedMajor.value = major

        // 시/도가 바뀌었으니 시/군/구 목록 갱신 & 기존 선택 초기화
        _middleList.value = regionManager.getMiddleRegions(major)
        _selectedMiddle.value = ""
    }

    // 시/군/구 선택 시 호출
    fun selectMiddle(middle: String) {
        _selectedMiddle.value = middle

        // 최종적으로 코드 찾기 등 수행
        val code = locationRepository.getRegionCode(_selectedMajor.value, middle)
        Timber.d("Selected Code: $code")
        // 쿼리 보내기
    }
}