package com.example.d104.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.d104.ui.component.PixelButtonCode

@Composable
fun MyPageScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEEEEE))
            .padding(20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        Text("1. 기본 버튼 (꽉 찬 너비)", fontSize = 16.sp, color = Color.Gray)
        PixelButtonCode(
            text = "게임 입장",
            onClick = { println("입장 클릭") },
            modifier = Modifier.fillMaxSize()
        )

        Text("2. 팀 선택 (색상 변경 + 너비 고정)", fontSize = 16.sp, color = Color.Gray)
        PixelButtonCode(
            text = "경찰 팀",
            onClick = { println("경찰 클릭") },
            blockWidth = 35,
            mainColor = Color(0xFF4B89DC),
            borderColor = Color(0xFF1A3B6E),
            textColor = Color.White
        )

        PixelButtonCode(
            text = "도둑 팀",
            onClick = { println("도둑 클릭") },
            blockWidth = 35,
            mainColor = Color(0xFFE74C3C),
            borderColor = Color(0xFF8E1818),
            textColor = Color(0xFFF1C40F)
        )

        Text("3. 크기 조절 (pixelSize)", fontSize = 16.sp, color = Color.Gray)
        PixelButtonCode(
            text = "취소",
            onClick = { println("취소 클릭") },
            pixelSize = 3.dp,
            blockWidth = 20,
            mainColor = Color.LightGray
        )

        PixelButtonCode(
            text = "왕 버튼",
            onClick = { println("왕 버튼 클릭") },
            pixelSize = 6.dp,
            blockWidth = 25
        )

        Text("4. 정사각형 (아이콘용)", fontSize = 16.sp, color = Color.Gray)
        PixelButtonCode(
            text = "X",
            onClick = { println("닫기") },
            blockWidth = 16,
            blockHeight = 16,
            mainColor = Color(0xFF333333),
            textColor = Color.White
        )

        Spacer(modifier = Modifier.height(50.dp))
    }
}
