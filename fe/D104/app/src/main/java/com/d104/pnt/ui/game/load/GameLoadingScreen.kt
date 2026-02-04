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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.R
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.ui.component.ContDownUI

@Composable
fun GameLoadingScreen(
    roomId: Long,
    role: GameRole,
    viewModel: GameLoadingViewModel = hiltViewModel(),
    onLoadingComplete: (gameId: Long) -> Unit
) {
    val remainingTime by viewModel.remainingTime.collectAsStateWithLifecycle()
    val isFinished by viewModel.isFinished.collectAsStateWithLifecycle()


    LaunchedEffect(isFinished) {
        if (isFinished) {
            onLoadingComplete(roomId)
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

            ContDownUI(
                remainingSeconds = remainingTime
            )

            Spacer(Modifier.height(24.dp))

            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                painter = painterResource(R.drawable.img_pnt_run),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )

            Text(
                text = role.description,
                color = Color.White
            )
        }
    }
}
