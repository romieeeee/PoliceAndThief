package com.d104.pnt.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.location.Location
import com.d104.pnt.domain.model.DraggableLatLng
import com.d104.pnt.domain.model.PlayerLocation
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

object LocationRepository {
    private val _currentLocation = MutableStateFlow<Location?>(null)
    private val _polygonPoints = MutableStateFlow<List<LatLng>>(emptyList())
    private val _prisonLocation = MutableStateFlow<LatLng?>(null)
    private val _playerLocations = MutableStateFlow<List<PlayerLocation>>(emptyList())

    // 외부(UI)에서는 읽기만 가능하도록 공개
    val currentLocation = _currentLocation.asStateFlow()
    val polygonPoints = _polygonPoints.asStateFlow()
    val prisonLocation = _prisonLocation.asStateFlow()
    val playerLocations = _playerLocations.asStateFlow()

    // 업데이트하는 함수
    fun updateCurrentLocation(location: Location) {
        _currentLocation.value = location
    }

    fun updatePlayerLocation(locations: List<PlayerLocation>) {
        _playerLocations.value = locations
    }

    // 바뀐 포인트 저장
    fun setPolygonPoints(points: List<LatLng>) {
        _polygonPoints.value = points
    }

    fun deletePolygonPoint(targetList: MutableList<DraggableLatLng>, index: Int): Boolean {

        // 1. 점이 3개보다 많고, 인덱스가 유효한지 확인
        if (targetList.size > 3 && index in targetList.indices) {
            targetList.removeAt(index)
            return true // 삭제 성공
        }

        return false // 삭제 실패 (3개 이하 등)
    }

    fun addPointToList(targetList: MutableList<DraggableLatLng>, newPoint: LatLng) {
        val insertIndex = getInsertionIndex(newPoint, targetList.map { it.position })
        targetList.add(insertIndex, DraggableLatLng(position = newPoint))
    }

    // 가장 가까운 점을 찾아 끼워넣는 함수
    private fun getInsertionIndex(point: LatLng, points: List<LatLng>): Int {
        var minDistance = Double.MAX_VALUE
        var insertIndex = points.size // 기본값 (못 찾으면 맨 뒤)

        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size] // 마지막 점 -> 첫 점 연결

            // PolyUtil을 사용한 정확한 거리 계산
            val distance = PolyUtil.distanceToLine(point, p1, p2)

            if (distance < minDistance) {
                minDistance = distance
                // p1과 p2 사이에 넣어야 하므로 index는 i + 1
                insertIndex = i + 1
            }
        }
        return insertIndex
    }

    // [NEW] 기준 위치를 받아 기본 사각형(정사각형) 생성 후 저장
    fun createDefaultPolygon(center: Location) {
        val lat = center.latitude
        val lng = center.longitude
        val offset = 0.001 // 약 100m

        val defaultPoints = listOf(
            LatLng(lat + offset, lng - offset), // 11시
            LatLng(lat + offset, lng + offset), // 1시
            LatLng(lat - offset, lng + offset), // 5시
            LatLng(lat - offset, lng - offset)  // 7시
        )
        _polygonPoints.value = defaultPoints
    }

    fun setPrisonLocation(location: LatLng?) {
        _prisonLocation.value = location
    }

    fun DismissCreateGame() {
        _polygonPoints.value = emptyList()
        _prisonLocation.value = null
    }

    // TODO: 더미 데이터 셋팅 지울것
    fun DummyPlayer() {
        _playerLocations.value = listOf(
            PlayerLocation(1, 101, 36.106996199409316, 128.41636536008272, 0, 1),
            PlayerLocation(1, 102, 36.10663643418624, 128.4164977111253, 0, 1),
            PlayerLocation(1, 100, 36.106996199409316, 128.41636536008272, 0, 1),
            PlayerLocation(1, 103, 36.106218602970124, 128.4160154777275, 0, 1),
            PlayerLocation(1, 104, 36.1069317248971, 128.41591167747148, 0, 2),
            PlayerLocation(1, 105, 36.106888405240205, 128.4159553194928, 3, 2),
            PlayerLocation(1, 106, 36.10686966460729, 128.41601606002928, 3, 2),
            PlayerLocation(1, 107, 36.10662012389394, 128.41578115461553, 2, 2),
            PlayerLocation(1, 108, 36.106996199409316, 128.41636536008272, 0, 2),
            PlayerLocation(1, 109, 36.10629276353168, 128.4166025880095, 1, 2),
            PlayerLocation(1, 110, 36.106996199409316, 128.41636536008272, 0, 2),
        )
    }
}