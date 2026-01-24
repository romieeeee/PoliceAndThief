package com.d104.pnt.ui.game.play

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.d104.pnt.R


@Composable
fun MissionFrame(
    screen: PhoneScreen,
) {
    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    ) {

        // 1. 휴대폰 프레임 이미지
        Image(
            painter = painterResource(R.drawable.phone),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Crop
        )

        // 2. 디스플레이 영역 (정확히 맞춘 영역)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.773f)          // 정확한 가로 비율
                .aspectRatio(593f / 874f)      // 실제 디스플레이 비율
                .offset(y = (-38).dp)          // 아래 설명
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {


        }
    }
}

@Composable
fun MissionListScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ThiefRow() }
        item { ThiefRow() }
        item { ThiefRow() }
        item { ThiefRow() }
        item { ThiefRow() }
        item { ThiefRow() }
        item { ThiefRow() }
        item { ThiefRow() }
    }
}


