package com.d104.pnt.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.ui.theme.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState

@Composable
fun PixelMarker(
    position: LatLng,
){
    MarkerComposable(
        state = MarkerState(position = position)
    ) {
        Image(
            painter = painterResource(id = R.drawable.map_marker_blue),
            contentDescription = "픽셀 커스텀 마커",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(widthDp = 50, heightDp = 50)
@Composable
fun PreviewPixelMarker(){
    PixelMarker(
        position = LatLng(37.566535, 126.977969)
    )
}