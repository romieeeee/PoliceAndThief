package com.d104.pnt.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.location.Location
import com.d104.pnt.domain.model.DraggableLatLng
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

object LocationRepository {
    private val _currentLocation = MutableStateFlow<Location?>(null)
    private val _polygonPoints = MutableStateFlow<List<LatLng>>(emptyList())
    private val _prisonLocation = MutableStateFlow<LatLng?>(null)



    // 외부(UI)에서는 읽기만 가능하도록 공개
    val currentLocation = _currentLocation.asStateFlow()
    val polygonPoints = _polygonPoints.asStateFlow()
    val prisonLocation = _prisonLocation.asStateFlow()




    // 서비스가 호출해서 위치를 업데이트하는 함수
    fun updateCurrentLocation(location: Location) {
        _currentLocation.value = location
    }

    // 바뀐 포인트 저장
    fun setPolygonPoints(points: List<LatLng>) {
        _polygonPoints.value = points
    }

    // 2-2. 특정 인덱스의 점만 업데이트 (드래그 할 때 실시간 호출)
    fun updatePolygonPoint(index: Int, newPoint: LatLng) {
        val currentList = _polygonPoints.value.toMutableList() // 복사본 생성
        if (index in currentList.indices) {
            currentList[index] = newPoint
            _polygonPoints.value = currentList // 새 리스트로 교체하여 UI 갱신 유도
        }
    }

    fun deletePolygonPoint(index: Int): Boolean {
        val currentList = _polygonPoints.value.toMutableList()

        // 1. 점이 3개보다 많고, 인덱스가 유효한지 확인
        if (currentList.size > 3 && index in currentList.indices) {
            currentList.removeAt(index)
            _polygonPoints.value = currentList // 리스트 갱신 -> UI 자동 반영
            return true // 삭제 성공
        }

        return false // 삭제 실패 (3개 이하 등)
    }

    fun addPointNaturally(newPoint: LatLng) {
        val currentList = _polygonPoints.value.toMutableList()

        // 1. 점이 3개 미만이면 그냥 뒤에 추가
        if (currentList.size < 3) {
            currentList.add(newPoint)
        } else {
            // 2. 작성해주신 로직을 활용하여 끼워넣을 위치 찾기
            val insertIndex = getInsertionIndex(newPoint, currentList)
            currentList.add(insertIndex, newPoint)
        }

        // 3. 갱신
        _polygonPoints.value = currentList
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
}