package com.d104.pnt.ui.game.play

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.ui.component.PixelContainer
import com.d104.pnt.ui.game.play.PhoneScreen.CAMERA
import com.d104.pnt.ui.game.play.PhoneScreen.MAP
import com.d104.pnt.ui.game.play.PhoneScreen.NO_SIGNAL
import com.d104.pnt.ui.game.play.PhoneScreen.THIEF_LIST
import com.d104.pnt.ui.theme.PastelBlue
import com.d104.pnt.ui.theme.WantedRed


@Composable
fun PhoneFrame(
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
            when (screen) {
                NO_SIGNAL -> ThiefListScreen()
                MAP -> MiniMapScreen()
                CAMERA -> CameraScanScreen()
                THIEF_LIST -> ThiefListScreen()
            }
        }
    }
}

@Composable
fun ThiefListScreen() {
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


@Composable
fun MiniMapScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {

    }
}

@Composable
fun CameraScanScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {

    }
}


@Composable
fun ThiefRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PixelContainer(
            modifier = Modifier.weight(1f),
            innerHorizontalPadding = 14,
            backgroundColor = PastelBlue,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_thief),
                    contentDescription = null,
                    tint = Color.Unspecified
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = "개구리중사래로로래로",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }


        }

        PixelContainer(
            innerHorizontalPadding = 10,
            backgroundColor = WantedRed,
            borderWidth = 1f
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(vertical = 2.dp),
                text = "검거",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

