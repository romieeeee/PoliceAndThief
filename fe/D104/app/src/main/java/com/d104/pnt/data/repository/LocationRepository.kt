package com.d104.pnt.data.repository

import android.location.Location
import com.d104.pnt.domain.model.DraggableLatLng
import com.d104.pnt.domain.model.GeoLocationInfo
import com.d104.pnt.domain.model.PlayerData
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.StateFlow

interface LocationRepository {
    val currentLocation: StateFlow<Location?>
    val polygonPoints: StateFlow<List<LatLng>>
    val prisonLocation: StateFlow<LatLng?>
    val playerLocations: StateFlow<List<PlayerData>>

    // 업데이트하는 함수
    fun updateCurrentLocation(location: Location)

    fun updatePlayerLocation(locations: List<PlayerData>)

    // 바뀐 포인트 저장
    fun setPolygonPoints(points: List<LatLng>)

    fun deletePolygonPoint(targetList: MutableList<DraggableLatLng>, index: Int): Boolean

    fun addPointToList(targetList: MutableList<DraggableLatLng>, newPoint: LatLng)

    // [NEW] 기준 위치를 받아 기본 사각형(정사각형) 생성 후 저장
    fun createDefaultPolygon(center: Location)

    fun setPrisonLocation(location: LatLng?)

    fun dismissCreateGame()

    suspend fun getAddressFromLatLng(latitude: Double, longitude: Double): GeoLocationInfo

    fun getRegionCode(major: String, middle: String): Int
}