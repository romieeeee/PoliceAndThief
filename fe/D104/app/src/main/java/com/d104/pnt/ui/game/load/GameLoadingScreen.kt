package com.d104.pnt.ui.game.load

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.ui.component.ContDownUI

@Composable
fun GameLoadingScreen(
    viewModel: GameLoadingViewModel,
    onLoadingComplete: (gameId: Long) -> Unit
) {
    val remainingTime by viewModel.remainingTime.collectAsState()
    val isFinished by viewModel.isFinished.collectAsState()

    LaunchedEffect(isFinished) {
        if (isFinished) {
            onLoadingComplete(1L) // TODO: 실제 gameId 연결
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 배경
        Image(
            painter = painterResource(id = R.drawable.bg_night),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // TODO: 경찰 도둑 애니메이션 (나중에 GIF로 교체 가능)

            ContDownUI(
                remainingSeconds = remainingTime
            )

            Spacer(Modifier.height(24.dp))

            Image(
                modifier = Modifier.fillMaxWidth().padding(30.dp),
                painter = painterResource(R.drawable.img_pnt_run),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    }
}
