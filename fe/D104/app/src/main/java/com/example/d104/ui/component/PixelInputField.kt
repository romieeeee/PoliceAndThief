package com.example.d104.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.d104.ui.theme.*

@Preview
@Composable
fun PixelInputField(
    modifier: Modifier = Modifier,
    placeholder: String = "아이디",
    backgroundColor: Color = PixelWhite,
    borderColor: Color = PixelBlack,
    width: Int = 100,
    height: Int = 50
) {
    var text by remember { mutableStateOf("") }

    // 배경 컨테이너
    PixelContainer(
        modifier = modifier,
        borderWidth = 5f,
        cornerSize = 20f,
        backgroundColor = backgroundColor,
        borderColor = borderColor
    ) {
        // 내부에 들어갈 입력 필드
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = PixelBlack,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 입력값이 비었을 때 '아이디' 라벨 표시
                    if (text.isEmpty()) {
                        Text(placeholder, color = Color.Gray)
                    }
                }
                // 실제 입력 필드 렌더링
                innerTextField()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}