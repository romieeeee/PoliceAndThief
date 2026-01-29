package com.d104.pnt.ui.chatroomlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.d104.pnt.data.remote.model.response.ChatSearchResponse
import com.d104.pnt.data.repository.ChatRepository
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.data.source.local.RegionCodeManager
import com.d104.pnt.domain.model.ChatRoomData
import com.d104.pnt.domain.model.common.BaseResult
import com.d104.pnt.domain.model.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChatRoomListViewModel @Inject constructor(
    private val regionManager: RegionCodeManager,
    private val locationRepository: LocationRepository,
    private val chatRepository: ChatRepository
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchRegionQuery = MutableStateFlow(-1)
    val searchRegionQuery = _searchRegionQuery.asStateFlow()

    private val _listState = MutableStateFlow<UiState<ChatSearchResponse>>(UiState.Idle)
    val listState = _listState.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.Me)
    val viewMode = _viewMode.asStateFlow()


    fun updateSearchQuery(newQuery: String) {
        _searchQuery.value = newQuery
    }

    // 시/도 선택 시 호출
    fun selectMajor(major: String) {
        initRegion()
        _selectedMajor.value = major
        if (_viewMode.value == ViewMode.Region) _viewMode.value = ViewMode.Title

        // 시/도가 바뀌었으니 시/군/구 목록 갱신 & 기존 선택 초기화
        _middleList.value = regionManager.getMiddleRegions(major)
        _selectedMiddle.value = ""
    }

    // 시/군/구 선택 시 호출
    fun selectMiddle(middle: String) {
        _selectedMiddle.value = middle
        _viewMode.value = ViewMode.Region

        // 최종적으로 코드 찾기 등 수행
        val code = locationRepository.getRegionCode(_selectedMajor.value, middle)
        Timber.d("Selected Code: $code")
        _searchRegionQuery.value = code

        // 쿼리 보내기
        viewModelScope.launch {
            _listState.value = UiState.Loading
            when (val result = chatRepository.searchChatRoom(title = null, regionCode = code)) {
                is BaseResult.Success -> {
                    _listState.value = UiState.Success(result.data)
                    Timber.d("chatList: ${result.data}")
                }
                is BaseResult.Error -> {
                    _listState.value = UiState.Error(result.error.message)
                    Timber.d("error: ${result.error.message}")
                }
            }
        }
    }
    fun initRegion() {
        _selectedMajor.value = ""
        _selectedMiddle.value = ""
        _middleList.value = emptyList()
        _searchRegionQuery.value = -1
    }

    fun searchChatRoom(
        title: String?,
        regionCode: Int?
    ) {
        if (_viewMode.value == ViewMode.Me) _viewMode.value = ViewMode.Title
        viewModelScope.launch {
            _listState.value = UiState.Loading
            when (val result = chatRepository.searchChatRoom(title, regionCode)) {
                is BaseResult.Success -> {
                    _listState.value = UiState.Success(result.data)
                    Timber.d("chatList: ${result.data}")
                }
                is BaseResult.Error -> {
                    _listState.value = UiState.Error(result.error.message)
                    Timber.d("error: ${result.error.message}")
                }
            }
        }
    }

    fun getJoinedChatRoom() {
        _viewMode.value = ViewMode.Me
        initRegion()
        viewModelScope.launch {
            _listState.value = UiState.Loading
            when (val result = chatRepository.getJoinedChatRoom()) {
                is BaseResult.Success -> {
                    _listState.value = UiState.Success(result.data)
                    Timber.d("chatList: ${result.data}")
                }
                is BaseResult.Error -> {
                    _listState.value = UiState.Error(result.error.message)
                    Timber.d("error: ${result.error.message}")
                }
            }

        }
    }

    enum class ViewMode(val value: String) {
        Me("Me"),
        Title("title"),
        Region("region");
    }
}