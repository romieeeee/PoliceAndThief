package com.d104.pnt.ui.game.wait

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.data.remote.model.request.Location
import com.d104.pnt.domain.model.DraggableLatLng
import com.d104.pnt.ui.component.GoogleMaps
import com.d104.pnt.ui.component.PixelButtonCode
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.theme.DarkSurface
import com.d104.pnt.ui.theme.DialogBorderColor
import com.d104.pnt.ui.theme.TextPrimary
import com.google.android.gms.maps.model.LatLng

@Composable
fun GameRoomMapSettingDialog(
    modifier: Modifier,
    onDismiss: () -> Unit,
    onConfirm: (Location, List<Location>) -> Unit,
    viewModel: GameRoomViewModel = hiltViewModel()
) {
    val originInfo by viewModel.roomInfo.collectAsStateWithLifecycle()
    val originPoints = originInfo.polygon ?: emptyList()
    val originPrisonLocation = originInfo.prison ?: Location(lat = 0.0, lng = 0.0)
    val tempPolygonPoints = remember(originPoints) {
        originPoints.map {
            DraggableLatLng(position = LatLng(it.lat, it.lng))
        }.toMutableStateList()
    }
    var prisonLocation by remember(originPrisonLocation) {
        mutableStateOf(LatLng(originPrisonLocation.lat, originPrisonLocation.lng))
    }

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        PixelContainer(
            modifier = Modifier,
            backgroundColor = DarkSurface,
            borderColor = DialogBorderColor,
        ) {
            Scaffold(
                modifier = Modifier,
                topBar = {
                    Text(
                        modifier = Modifier.padding(10.dp),
                        text = "경기 구역 수정",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                },
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        PixelButtonCode(
                            text = "취소",
                            onClick = { onDismiss() },
                            modifier = Modifier.weight(1f),
                            mainColor = Color.White,
                            borderColor = Color.Black,
                            textColor = Color.Black,
                            fontSize = 16,
                            blockHeight = 13
                        )

                        PixelButtonCode(
                            text = "확인",
                            onClick = {
                                onConfirm(
                                    Location(prisonLocation.latitude, prisonLocation.longitude),
                                    tempPolygonPoints.map {
                                        Location(it.position.latitude, it.position.longitude)
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f),
                            mainColor = Color.White,
                            borderColor = Color.Black,
                            textColor = Color.Black,
                            fontSize = 16,
                            blockHeight = 13
                        )
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GoogleMaps(
                        modifier = Modifier.weight(1f),
                        inGameMinimap = false,
                        isPreview = false,
                        polygonPoints = tempPolygonPoints,
                        onPointChange = { index, newPos ->
                            if (index in tempPolygonPoints.indices) {
                                tempPolygonPoints[index] =
                                    tempPolygonPoints[index].copy(position = newPos)
                            }
                        },
                        onPointDelete = { index ->
                            viewModel.deletePolygonPoint(tempPolygonPoints, index)
                        },
                        onAddPoint = { newPoint ->
                            viewModel.addPointToList(tempPolygonPoints, newPoint)
                        },
                        prisonLocation = prisonLocation,
                        onPrisonChange = { newLoc ->
                            prisonLocation = newLoc
                        }
                    )
                    Text(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        text = "- 마커를 꾹 눌러 드래그로 경기구역을 수정할 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                    Text(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        text = "- 감옥을 드래그하여 이동시킬 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}