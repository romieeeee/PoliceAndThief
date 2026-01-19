package com.example.d104.ui.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MyPageScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Blue)
        ) {
            Text(text = "프로필 카드", color = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.5f)
                .background(Color.Magenta)
        ) {
            Text(text = "전적 요약", color = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f)
                .background(Color.Cyan)
        ) {
            Text(text = "역할 선택")
        }
    }
}