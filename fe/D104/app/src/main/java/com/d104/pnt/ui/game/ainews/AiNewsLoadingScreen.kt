package com.d104.pnt.ui.game.ainews

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.d104.pnt.R
import com.d104.pnt.ui.theme.PixelFont
import kotlinx.coroutines.delay

@Composable
fun AiNewsLoadingScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 이미지
        Image(
            painter = painterResource(id = R.drawable.img_waitingroom),
            contentDescription = "방송 대기실 배경",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-50).dp)
        )
        // 배경 명도 조절(어둡게)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )
        // 아나운서, 멘트
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val context = LocalContext.current

            // 아나운서 GIF
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.drawable.news_anchor)
                    .decoderFactory { result, options, _ ->
                        if (Build.VERSION.SDK_INT >= 28) {
                            ImageDecoderDecoder(result.source, options)
                        } else {
                            GifDecoder(result.source, options)
                        }
                    }
                    .build(),
                contentDescription = "방송 준비 중인 아나운서",
                placeholder = painterResource(R.drawable.example_anchor),    // 프리뷰용 임시 이미지
                error = painterResource(R.drawable.example_anchor),       // 에러 시 보여줄 이미지
                modifier = Modifier.size(265.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            TypewriterText(
                texts = listOf(
                    "AI가 경기 데이터를 분석하고 있습니다...",
                    "결과를 집계중입니다...",
                    "잠시 후 뉴스가 시작됩니다..."
                )
            )
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            NewsTickerBar(
                text = "속보: 도심 추격 상황 종료... 경찰청, 검거 현황 브리핑 준비 중... 현장 CCTV 정밀 분석 영상 입수... 잠시 후 단독 보도!      "
            )
        }
    }
}

@Composable
fun TypewriterText(texts: List<String>) {
    var textIndex by remember { mutableIntStateOf(0) }
    var textToDisplay by remember { mutableStateOf("") }

    LaunchedEffect(textIndex) {
        val fullText = texts[textIndex]

        for (i in 1..fullText.length) {
            textToDisplay = fullText.substring(0, i) + " ▌"
            delay(100)
        }

        delay(1500)

        textToDisplay = ""
        textIndex = (textIndex + 1) % texts.size
    }

    Text(
        text = textToDisplay,
        color = Color.White,
        fontFamily = PixelFont,
        fontSize = 20.sp,
        lineHeight = 30.sp
    )
}


@Preview
@Composable
fun AiNewsLoadingPreview() {
    AiNewsLoadingScreen()
}