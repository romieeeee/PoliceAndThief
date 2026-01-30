package com.d104.pnt.ui.game.wait

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.d104.pnt.domain.model.GameRole

@Composable
fun GameWaitingScreen(
    roomId: Long,
    onStartGame: (Long, GameRole) -> Unit,
    onBackPressed: () -> Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {

    }
}