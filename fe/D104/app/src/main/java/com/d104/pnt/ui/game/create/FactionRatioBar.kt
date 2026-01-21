package com.d104.pnt.ui.game.create

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d104.pnt.R
import com.d104.pnt.ui.theme.AccentRed
import com.d104.pnt.ui.theme.ButtonPrimary

@Composable
fun FactionRatioBar(
    totalCount: Int,
    policeCount: Int,
    thiefCount: Int,
    onPoliceCountChange: (Int) -> Unit
) {
    var barWidth by remember { mutableStateOf(0f) }

    fun calculateCountFromX(x: Float): Int {
        if (barWidth <= 0) return policeCount
        val ratio = (x / barWidth).coerceIn(0f, 1f)
        val count = (ratio * totalCount).toInt()
        return count.coerceIn(1, totalCount - 1)
    }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                // 너비 측정
                .onSizeChanged { barWidth = it.width.toFloat() }
                // 탭 인식
                .pointerInput(totalCount) {
                    detectTapGestures { offset ->
                        val newCount = calculateCountFromX(offset.x)
                        onPoliceCountChange(newCount)
                    }
                } // 드래그 인식
                .pointerInput(totalCount) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val newCount = calculateCountFromX(change.position.x)
                        onPoliceCountChange(newCount)
                    }
                }
                .background(AccentRed)
        ) {
            // 경찰 비율 계산
            val policeRatio =
                if (totalCount > 0) policeCount.toFloat() / totalCount.toFloat() else 0f

            // 경찰 영역
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(policeRatio)
                    .background(ButtonPrimary)
            ) {
                // 흰색 구분선
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 경찰 아이콘 & 숫자
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_police),
                        contentDescription = "경찰 아이콘",
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = (-18).dp),
                        contentScale = ContentScale.Fit
                    )

                    Text(
                        text = "$policeCount",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black, blurRadius = 2f)
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // 도둑 아이콘 & 숫자
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$thiefCount",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black, blurRadius = 2f)
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    Image(
                        painter = painterResource(id = R.drawable.img_thief),
                        contentDescription = "도둑 아이콘",
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = 18.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}