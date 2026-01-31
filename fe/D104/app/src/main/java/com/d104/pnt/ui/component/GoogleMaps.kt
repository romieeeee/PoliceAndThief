package com.d104.pnt.ui.component

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
import androidx.compose.ui.unit.dp
import com.d104.pnt.BuildConfig
import com.d104.pnt.R
import com.d104.pnt.domain.model.DraggableLatLng
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.domain.model.PlayerData
import com.d104.pnt.ui.theme.AreaBoundary
import com.d104.pnt.ui.theme.InArea
import com.d104.pnt.ui.theme.OutOfArea
import com.d104.pnt.ui.theme.PrisonArea
import com.d104.pnt.ui.theme.PrisonBoundary
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

@Composable
fun GoogleMaps(
    modifier: Modifier,
    currentLocation: LatLng = LatLng(37.56681969564895, 126.97864094105321),
    playerLocations: List<PlayerData> = emptyList(),
    prisonLocation: LatLng? = null,
    inGameMinimap: Boolean = false,
    isPreview: Boolean = true,
    polygonPoints: List<DraggableLatLng>,
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
        position = CameraPosition.fromLatLngZoom(currentLocation, 17f)
    }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(polygonPoints) {
        if (polygonPoints.isNotEmpty() && isPreview) {
            val builder = LatLngBounds.builder()
            polygonPoints.forEach { builder.include(it.position) }

            try {
                val bounds = builder.build()
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
            zoomGesturesEnabled = !inGameMinimap && !isPreview,
            scrollGesturesEnabled = !inGameMinimap && !isPreview,
            zoomControlsEnabled = false,
            myLocationButtonEnabled = !inGameMinimap && !isPreview,
            mapToolbarEnabled = false,
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
            if (!inGameMinimap && !isPreview) onAddPoint(latLng)
            true
        }
    ) {
        if (inGameMinimap && isPreview) {
            PixelMarker(
                location = LatLng(currentLocation.latitude, currentLocation.longitude),
                position = "ME"
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
                holes = listOf(polygonPoints.map { it.position })
            )
            if (prisonLocation != null) {
                Circle(
                    center = prisonLocation,
                    radius = 20.0,
                    fillColor = PrisonArea,
                    strokeColor = PrisonBoundary,
                    strokeWidth = 5f,
                )
            }
            if (role == GameRole.POLICE) {
                val MY_MEMBER_ID = 100L
                playerLocations.toList().forEach {
                    if (it.memberId != MY_MEMBER_ID) {
                        if (it.status != "THIEF") {
                            PixelMarker(
                                location = LatLng(it.lat, it.lng),
                                position = it.position
                            )
                        }
                    }
                }
            }
        } else if (!inGameMinimap && !isPreview) { // 방장 지도 수정 뷰
            if (polygonPoints.isNotEmpty() && prisonLocation != null) {
                Polygon(
                    points = polygonPoints.map { it.position },
                    fillColor = InArea,
                    strokeColor = AreaBoundary,
                    strokeWidth = 5f
                )
                Circle(
                    center = prisonLocation,
                    radius = 20.0,
                    fillColor = PrisonArea,
                    strokeColor = PrisonBoundary,
                    strokeWidth = 5f,
                )
                polygonPoints.toList().forEachIndexed { index, item ->
                    key(item.id) {
                        val markerState =
                            rememberMarkerState(key = index.toString(), position = item.position)

                        LaunchedEffect(markerState.position) {
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
                            draggable = true,
                            anchor = Offset(0.5f, 0.5f),
                            onClick = {
                                if (!onPointDelete(index)) {
                                    Toast.makeText(context, "최소 3개의 점이 필요합니다.", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                true
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
                                    draggingIndex = index
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }

                                DragState.END -> draggingIndex = -1
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
                    draggable = true,
                    anchor = Offset(0.5f, 0.5f),
                    title = "감옥",
                    onClick = {
                        true
                    }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.map_marker_prison_temp),
                        contentDescription = "감옥",
                        modifier = Modifier.size(35.dp)
                    )
                }
            }
        } else if (!inGameMinimap && isPreview) {
            if (polygonPoints.isNotEmpty() && prisonLocation != null) {
                Polygon(
                    points = polygonPoints.map { it.position },
                    fillColor = InArea,
                    strokeColor = AreaBoundary,
                    strokeWidth = 5f
                )
                Circle(
                    center = prisonLocation,
                    radius = 20.0,
                    fillColor = PrisonArea,
                    strokeColor = PrisonBoundary,
                    strokeWidth = 5f,
                )
            }
        }
    }
}