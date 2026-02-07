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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d104.pnt.R
import com.d104.pnt.domain.model.GameRole
import com.d104.pnt.ui.component.ContDownUI
import com.d104.pnt.ui.theme.PixelFont
import com.d104.pnt.ui.component.GifImage

@Composable
fun GameLoadingScreen(
    roomId: Long,
    role: GameRole,
    viewModel: GameLoadingViewModel = hiltViewModel(),
    onLoadingComplete: (gameId: Long) -> Unit
) {
    val remainingTime by viewModel.remainingTime.collectAsStateWithLifecycle()
    val isFinished by viewModel.isFinished.collectAsStateWithLifecycle()

    val message by viewModel.message.collectAsStateWithLifecycle()

    LaunchedEffect(isFinished) {
        if (isFinished) {
            onLoadingComplete(roomId)
        }
    }

    val timerLabel = when (role) {
        GameRole.POLICE -> "작전 투입까지"
        GameRole.THIEF -> "경찰 출동까지"
        else -> "게임 시작까지"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GifImage(
            modifier = Modifier.fillMaxSize(),
            imageRes = R.drawable.gif_running
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = timerLabel,
                fontFamily = PixelFont,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            ContDownUI(
                remainingSeconds = remainingTime
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = role.description,
                color = Color.White
            )
        }
    }
}
