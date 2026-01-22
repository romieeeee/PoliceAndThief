package com.d104.pnt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.d104.pnt.ui.theme.*

// 상태 정의
enum class NetworkErrorState {
    RECONNECTING,
    FAILED
}

@Composable
fun NetworkErrorScreen(
    state: NetworkErrorState,
    retryCount: Int,
    onRetry: () -> Unit,
    onGoToMain: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(
        onDismissRequest = { }, // 뒤로가기 버튼 막음
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                NetworkErrorState.RECONNECTING -> {
                    PixelLoading(
                        size = 64,
                        message = "재접속중입니다...",
                        fontSize = 18.sp,
                        textColor = TextPrimary
                    )
                }
                NetworkErrorState.FAILED -> {
                    ErrorPopup(
                        onRetry = onRetry,
                        onGoToMain = onGoToMain,
                        onExit = onExit
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorPopup(
    onRetry: () -> Unit,
    onGoToMain: () -> Unit,
    onExit: () -> Unit
) {
    PixelContainer(
        modifier = Modifier.fillMaxWidth(0.85f),
        backgroundColor = Color(0xFF374151),
        borderColor = Color.Gray
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "네트워크 접속 실패",
                color = AccentYellow,
                fontFamily = PixelFont,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "서버와의 연결이 끊겼습니다.",
                color = TextPrimary,
                fontFamily = PixelFont,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PixelButtonCode(
                    text = "재접속 시도",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    mainColor = Color.White,
                    textColor = Color.Black,
                    blockHeight = 12,
                    fontSize = 14
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PixelButtonCode(
                            text = "메인으로",
                            onClick = onGoToMain,
                            modifier = Modifier.fillMaxWidth(),
                            mainColor = Color.White,
                            textColor = Color.Black,
                            blockHeight = 12,
                            fontSize = 14
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        PixelButtonCode(
                            text = "접속종료",
                            onClick = onExit,
                            modifier = Modifier.fillMaxWidth(),
                            mainColor = Color.White,
                            textColor = Color.Black,
                            blockHeight = 12,
                            fontSize = 14
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ReconnectingPreview() {
    NetworkErrorScreen(
        state = NetworkErrorState.RECONNECTING,
        retryCount = 2,
        onRetry = {}, onGoToMain = {}, onExit = {}
    )
}

@Preview(showBackground = true)
@Composable
fun FailedPreview() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        NetworkErrorScreen(
            state = NetworkErrorState.FAILED,
            retryCount = 0,
            onRetry = {}, onGoToMain = {}, onExit = {}
        )
    }
}