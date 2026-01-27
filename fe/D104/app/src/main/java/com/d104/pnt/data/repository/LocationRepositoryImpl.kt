package com.d104.pnt.data.repository

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.d104.pnt.domain.model.DraggableLatLng
import com.d104.pnt.domain.model.GeoLocationInfo
import com.d104.pnt.domain.model.PlayerLocation
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {
    // 내부 수정용 MutableStateFlow
    private val _currentLocation = MutableStateFlow<Location?>(null)
    private val _polygonPoints = MutableStateFlow<List<LatLng>>(emptyList())
    private val _prisonLocation = MutableStateFlow<LatLng?>(null)
    private val _playerLocations = MutableStateFlow<List<PlayerLocation>>(emptyList())

    // 인터페이스 구현 (외부 공개용)
    override val currentLocation = _currentLocation.asStateFlow()
    override val polygonPoints = _polygonPoints.asStateFlow()
    override val prisonLocation = _prisonLocation.asStateFlow()
    override val playerLocations = _playerLocations.asStateFlow()


    override fun updateCurrentLocation(location: Location) {
        _currentLocation.value = location
    }

    override fun updatePlayerLocation(locations: List<PlayerLocation>) {
        _playerLocations.value = locations
    }

    override fun setPolygonPoints(points: List<LatLng>) {
        _polygonPoints.value = points
    }

    override fun deletePolygonPoint(targetList: MutableList<DraggableLatLng>, index: Int): Boolean {
        if (targetList.size > 3 && index in targetList.indices) {
            targetList.removeAt(index)
            return true
        }
        return false
    }

    override fun addPointToList(targetList: MutableList<DraggableLatLng>, newPoint: LatLng) {
        // 내부 private 함수 활용
        val insertIndex = getInsertionIndex(newPoint, targetList.map { it.position })
        targetList.add(insertIndex, DraggableLatLng(position = newPoint))
    }

    // 이 함수는 인터페이스에 없고 내부에서만 쓰이므로 private 유지
    private fun getInsertionIndex(point: LatLng, points: List<LatLng>): Int {
        var minDistance = Double.MAX_VALUE
        var insertIndex = points.size

        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]
            val distance = PolyUtil.distanceToLine(point, p1, p2)

            if (distance < minDistance) {
                minDistance = distance
                insertIndex = i + 1
            }
        }
        return insertIndex
    }

    override fun createDefaultPolygon(center: Location) {
        val lat = center.latitude
        val lng = center.longitude
        val offset = 0.001

        val defaultPoints = listOf(
            LatLng(lat + offset, lng - offset),
            LatLng(lat + offset, lng + offset),
            LatLng(lat - offset, lng + offset),
            LatLng(lat - offset, lng - offset)
        )
        _polygonPoints.value = defaultPoints
    }

    override fun setPrisonLocation(location: LatLng?) {
        _prisonLocation.value = location
    }

    override fun dismissCreateGame() {
        _polygonPoints.value = emptyList()
        _prisonLocation.value = null
    }

    override suspend fun getAddressFromLatLng(latitude: Double, longitude: Double): GeoLocationInfo {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.KOREA) // 한국어로 설정

                // 지오코더 사용 불가능한 경우 (구글 서비스 미설치 등)
                if (!Geocoder.isPresent()) {
                    return@withContext GeoLocationInfo("", "", "")
                }

                // API 33 (Tiramisu) 이상: 리스너 방식 사용 (비동기)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // suspendCoroutine을 사용해 콜백을 코루틴 스타일로 변환
                    return@withContext suspendCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                Timber.d("addresses: $addresses")
                                // 1. Major (시/도)
                                // adminArea가 보통 '서울특별시', '경상북도' 등을 가짐
                                val major = address.adminArea ?: ""

                                // 2. Middle (시/구/군)
                                // subLocality가 있으면(강남구, 분당구 등) 그걸 쓰고,
                                // 없으면 locality(구미시, 수원시 등)를 씀.
                                // 서울의 경우 locality도 '서울'인 경우가 있어서 subLocality 우선 체크가 중요함.
                                val middle = address.subLocality ?: address.locality ?: ""

                                // 3. Sub (동/읍/면 or 도로명)
                                // thoroughfare가 보통 '도로명(대학로)' 또는 '동(역삼동)'을 가짐.
                                val sub = address.thoroughfare ?: ""
                                continuation.resume(GeoLocationInfo(major, middle, sub))
                            } else {
                                continuation.resume(GeoLocationInfo("", "", ""))
                            }
                        }
                    }
                }
                // API 33 미만: 기존 방식 사용 (동기 blocking)
                else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        // 1. Major (시/도)
                        val major = address.adminArea ?: ""

                        // 2. Middle (시/구/군)
                        // subLocality가 있으면(강남구, 분당구 등) 그걸 쓰고,
                        // 없으면 locality(구미시, 수원시 등)를 씀.
                        // 서울의 경우 locality도 '서울'인 경우가 있어서 subLocality 우선 체크가 중요함.
                        val middle = address.subLocality ?: address.locality ?: ""

                        // 3. Sub (동/읍/면 or 도로명)
                        // thoroughfare가 보통 '도로명(대학로)' 또는 '동(역삼동)'을 가짐.
                        val sub = address.thoroughfare ?:  ""

                        return@withContext GeoLocationInfo(major, middle, sub)
                    } else {
                        return@withContext GeoLocationInfo("", "", "")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext GeoLocationInfo("주소", "변환", "에러")
            }
        }
    }

    override fun DummyPlayer() {
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