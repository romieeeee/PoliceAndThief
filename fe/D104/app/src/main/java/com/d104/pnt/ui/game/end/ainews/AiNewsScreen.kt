package com.d104.pnt.ui.game.end.ainews

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d104.pnt.R
import com.d104.pnt.ui.theme.PixelFont

@Composable
fun AiNewsScreen(
    newsContent: String = "속보입니다. 오늘 새벽 도심 전역에서 벌어진 경찰과 도둑 간의 추격전이 경찰의 승리로 마무리됐습니다. 이번 작전으로 시민 피해는 발생하지 않았으며...속보입니다. 오늘 새벽 도심 전역에서 벌어진 경찰과 도둑 간의 추격전이 경찰의 승리로 마무리됐습니다. 이번 작전으로 시민 피해는 발생하지 않았으며...속보입니다. 오늘 새벽 도심 전역에서 벌어진 경찰과 도둑 간의 추격전이 경찰의 승리로 마무리됐습니다. 이번 작전으로 시민 피해는 발생하지 않았으며...속보입니다. 오늘 새벽 도심 전역에서 벌어진 경찰과 도둑 간의 추격전이 경찰의 승리로 마무리됐습니다. 이번 작전으로 시민 피해는 발생하지 않았으며...",
    onNextClick: () -> Unit = {}
) {
    var showSkipDialog by remember { mutableStateOf(false) }

    BackHandler { showSkipDialog = true }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_breaking_news),
            contentDescription = "뉴스 스튜디오",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 380.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NewsAnchor(modifier = Modifier.size(240.dp), isSpeaking = true)

            NewsScriptBox(content = newsContent)
        }

        // 하단 뉴스바 내용(경찰이 도둑 검거 or 도둑이 경찰의 포위망을 벗어나 탈출 성공 이런내용이면 좋을듯)
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            NewsTickerBar(text = " [속보] 경찰, 도둑 N명 검거 성공...내용내용내용내용내용 ")
        }

        SkipButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp),
            onClick = { showSkipDialog = true }
        )

        if (showSkipDialog) {
            SkipConfirmationDialog(
                onConfirm = {
                    showSkipDialog = false
                    onNextClick()
                },
                onDismiss = { showSkipDialog = false }
            )
        }
    }
}

@Composable
private fun SkipButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = "SKIP >>",
        color = Color.White,
        fontFamily = PixelFont,
        fontSize = 18.sp,
        modifier = modifier
            .clickable { onClick() }
            .padding(12.dp)
    )
}