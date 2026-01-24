package com.d104.pnt.domain.model

import com.google.android.gms.maps.model.LatLng
import java.util.UUID

data class DraggableLatLng(
    val id: String = UUID.randomUUID().toString(), // 생성될 때 고유 ID 부여
    val position: LatLng
)
