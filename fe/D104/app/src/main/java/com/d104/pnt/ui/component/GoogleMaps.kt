package com.d104.pnt.ui.component

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.BuildConfig
import com.d104.pnt.R
import com.d104.pnt.data.repository.LocationRepository
import com.d104.pnt.domain.model.DraggableLatLng
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.DragState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable //
fun GoogleMaps(
    modifier: Modifier,
    startLat: Double = 37.566535,
    startLng: Double = 126.977969,
    inGameMinimap: Boolean = false,
    isPreview: Boolean = true,
    polygonPoints: List<DraggableLatLng>,
    prisonLocation: LatLng? = null,
    role: GameRole = GameRole.POLICE,
    onPointChange: (Int, LatLng) -> Unit = { _, _ -> },
    onPointDelete: (Int) -> Boolean = { false },
    onAddPoint: (LatLng) -> Unit = {},
    onPrisonChange: (LatLng) -> Unit = {}
) {
    val inGameMapId = BuildConfig.INGAME_MAP_ID
    val settingMapId = BuildConfig.SETTING_MAP_ID

    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(startLat, startLng), 17f)
    }
    val userLocation = LatLng(startLat, startLng)
    var draggingIndex by remember { mutableIntStateOf(-1) }
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(polygonPoints) {
        if (polygonPoints.isNotEmpty() && isPreview) {
            val builder = LatLngBounds.builder()
            polygonPoints.forEach { builder.include(it.position) }

            try {
                val bounds = builder.build()
                // 미리보기(isPreview)일 때는 여백을 조금 더 줌 (padding: 50)
                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 50))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    GoogleMap(
        modifier = modifier
            .fillMaxSize(),
        uiSettings = MapUiSettings(
            zoomGesturesEnabled = !inGameMinimap && !isPreview,    // 미니맵이면 false
            scrollGesturesEnabled = !inGameMinimap && !isPreview,
            zoomControlsEnabled = false,
            myLocationButtonEnabled = !inGameMinimap && !isPreview,
            mapToolbarEnabled = false, // 항상 끄기
            compassEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false
        ),
        properties = MapProperties(
            maxZoomPreference = 20.0f,
            minZoomPreference = if (isPreview) 10.0f else 16.0f,
        ),
        googleMapOptionsFactory = {
            GoogleMapOptions()
                .mapId(if (inGameMinimap) inGameMapId else settingMapId)
        },
        cameraPositionState = cameraPositionState,
        onMapClick = { latLng ->
            // 편집모드일 때만 마커 추가함수 실행
            if (!inGameMinimap && !isPreview) onAddPoint(latLng)
            true
        }
    ) {
        if (inGameMinimap && isPreview) { // 인게임 뷰
            val currentLocation by LocationRepository.currentLocation.collectAsStateWithLifecycle()
            val playerLocations by LocationRepository.playerLocations.collectAsStateWithLifecycle()
            PixelMarker(
                position = LatLng(currentLocation!!.latitude, currentLocation!!.longitude),
                status = "ME"
            )
            Polygon( // 구역 밖을 표시하기 위한 폴리곤
                points = listOf(
                    LatLng(39.0, 130.0),
                    LatLng(32.0, 130.0),
                    LatLng(32.0, 125.0),
                    LatLng(39.0, 125.0),
                    ),
                fillColor = OutOfArea,
                strokeColor = PrisonBoundary,
                strokeWidth = 5f,
                holes = listOf(polygonPoints.map{it.position})
            )
            if (prisonLocation != null) {
                Circle(
                    center = prisonLocation,
                    radius = 20.0,
                    fillColor = PrisonArea, // 반투명 빨강
                    strokeColor = PrisonBoundary,
                    strokeWidth = 5f,
                )
            }
            if (role == GameRole.POLICE) {
                // 여러 마커 테스트용 더미 멤버아이디, 더미 플레이어
                val MY_MEMBER_ID = 100
                LocationRepository.DummyPlayer()
                playerLocations.toList().forEach {
                    if (it.member_id != MY_MEMBER_ID) {
                        val playerStatus = when (it.position) { // position 1은 경찰, 2는 도둑 가정
                            1 -> "POLICE"
                            else -> {
                                when (it.status) {
                                    1 -> "THIEF" // 1은 드러난 도둑, 2, 3은 체포된 도둑
                                    2 -> "ARRESTED"  // 0은 숨어있는 도둑이라고 가정
                                    3 -> "PRISONER"
                                    else -> "HIDE"
                                }
                            }
                        }
                        if (playerStatus != "HIDE") {
                            PixelMarker(
                                position = LatLng(it.latitude, it.longitude),
                                status = playerStatus
                            )
                        }
                        Log.d("PlayerLocations", "${it.member_id} : $playerStatus")
                    }
                }
            }
        }
        else if (!inGameMinimap && !isPreview) { // 방장 지도 수정 뷰
            if (polygonPoints.isNotEmpty() && prisonLocation != null) {
                Polygon(
                    points = polygonPoints.map { it.position },
                    fillColor = InArea, // 반투명 초록
                    strokeColor = AreaBoundary,
                    strokeWidth = 5f
                )
                Circle(
                    center = prisonLocation,
                    radius = 20.0,
                    fillColor = PrisonArea, // 반투명 빨강
                    strokeColor = PrisonBoundary,
                    strokeWidth = 5f,
                )
                polygonPoints.toList().forEachIndexed { index, item ->
                    key(item.id) {
                        // 마커의 위치 상태 관리
                        val markerState =
                            rememberMarkerState(key = index.toString(), position = item.position)

                        // [핵심] 마커가 움직이면 -> MapSettingDialog에 알림
                        LaunchedEffect(markerState.position) {
                            // 드래그 중일때만 업데이트
                            if (draggingIndex == index) {
                                onPointChange(index, markerState.position)
                            }
                        }

                        LaunchedEffect(item.position) {
                            if (draggingIndex != index || markerState.position != item.position) {
                                markerState.position = item.position
                            }
                        }

                        MarkerComposable(
                            state = markerState,
                            draggable = true, // 드래그 가능!
                            anchor = Offset(0.5f, 0.5f),
                            onClick = {
                                if (!onPointDelete(index)) {
                                    Toast.makeText(context, "최소 3개의 점이 필요합니다.", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                true // true를 반환해야 지도 기본 클릭 동작(카메라 이동 등)을 막음
                            }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.map_marker_white),
                                contentDescription = "픽셀 커스텀 마커",
                                modifier = Modifier.size(25.dp)
                            )
                        }

                        LaunchedEffect(markerState.dragState) {
                            when (markerState.dragState) {
                                DragState.DRAG -> {
                                    draggingIndex = index // 드래그 시작
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }

                                DragState.END -> draggingIndex = -1     // 드래그 끝
                                else -> {}
                            }
                        }
                    }
                }
                val prisonMarkerState = rememberMarkerState(position = prisonLocation)
                LaunchedEffect(prisonMarkerState.position) {
                    when (prisonMarkerState.dragState) {
                        DragState.START -> {
                            val isContain = PolyUtil.containsLocation(
                                prisonMarkerState.position,
                                polygonPoints.map { it.position }.toList(),
                                false
                            )
                            if (prisonLocation != prisonMarkerState.position && isContain) {
                                onPrisonChange(prisonMarkerState.position)
                            }
                        }

                        DragState.DRAG -> {
                            val isContain = PolyUtil.containsLocation(
                                prisonMarkerState.position,
                                polygonPoints.map { it.position }.toList(),
                                false
                            )
                            if (prisonLocation != prisonMarkerState.position && isContain) {
                                onPrisonChange(prisonMarkerState.position)
                            }
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }

                        DragState.END -> {
                            prisonMarkerState.position = prisonLocation
                        }

                        else -> {}
                    }
                }
                LaunchedEffect(prisonLocation) {
                    // 부모 값이 바뀌었는데 내 마커랑 다르면 이동 (초기화 or 외부 변경 시)
                    if (prisonMarkerState.position != prisonLocation) {
                        prisonMarkerState.position = prisonLocation
                    }
                }
                MarkerComposable(
                    state = prisonMarkerState,
                    draggable = true, // 드래그 가능
                    anchor = Offset(0.5f, 0.5f), // 이미지 중심
                    title = "감옥",
                    onClick = {
                        // 감옥은 삭제 기능이 없다면 false 반환 (지도 클릭 통과 방지용 true)
                        true
                    }
                ) {
                    // 감옥 아이콘 (기존 마커와 구분되게 다른 이미지나 색상 사용 추천)
                    Image(
                        painter = painterResource(id = R.drawable.map_marker_prison_temp), // 빨간 마커 예시
                        contentDescription = "감옥",
                        modifier = Modifier.size(35.dp) // 조금 더 크게
                    )
                }
            }
        }
        else if (!inGameMinimap && isPreview) { // 일반 유저 대기방 뷰
            if (polygonPoints.isNotEmpty() && prisonLocation != null) {
                Polygon(
                    points = polygonPoints.map { it.position },
                    fillColor = InArea, // 반투명 초록
                    strokeColor = AreaBoundary,
                    strokeWidth = 5f
                )
                Circle(
                    center = prisonLocation,
                    radius = 20.0,
                    fillColor = PrisonArea, // 반투명 빨강
                    strokeColor = PrisonBoundary,
                    strokeWidth = 5f,
                )
            }
        }
    }
}